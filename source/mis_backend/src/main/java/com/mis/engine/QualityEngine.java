package com.mis.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mis.entity.MisInsuredPerson;
import com.mis.entity.MisBill;
import com.mis.entity.MisCatalog;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MifQualityRule;
import com.mis.mapper.MisInsuredPersonMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.mis.service.CatalogSplitService;

/**
 * 结算清单质控引擎：对结算单费用明细逐条执行质控规则，产出质控命中。
 * <p>规则按 mif_quality_rule.rule_type 分派，阈值从 params（JSON）读取。
 */
@Component
@RequiredArgsConstructor
public class QualityEngine {

    private final ObjectMapper objectMapper;
    private final MisInsuredPersonMapper personMapper;
    private final com.mis.mapper.MisCatalogMapper catalogMapper;
    private final CatalogSplitService catalogSplitService;

    /** 质控命中 */
    @Data
    public static class Hit {
        private Long billId;
        private String billNo;
        private Long itemId;
        private String itemName;
        private String ybCode;
        private String medinsCode;
        private String medinsName;
        private Long ruleId;
        private String ruleCode;
        private String ruleName;
        private String ruleType;
        private Integer severity;
        private String fieldName;
        private String originValue;
        private String qualityDesc;
        private BigDecimal involveAmount;
        private BigDecimal deductAmount;
    }

    /** 一次质控内复用的数据 */
    @Data
    public static class Ctx {
        private Map<String, MisCatalog> catalogByCode = new HashMap<>();
        /** 同结算单内按项目名+类别聚合（重复上报判定用） */
        private Map<Long, List<MisFeeItem>> itemsByBill = new HashMap<>();
        /** 身份证 → 参保人性别（性别限制类质控用） */
        private Map<String, String> genderByIdCard = new HashMap<>();
        /** 目录分表限价（药品/诊疗/耗材分表为权威来源） */
        private Map<String, BigDecimal> limitPriceByCode = new HashMap<>();
    }

    /**
     * 装载质控上下文：目录、参保人性别。
     */
    public Ctx loadContext() {
        Ctx ctx = new Ctx();
        try {
            for (com.mis.entity.MisCatalog c : catalogMapper.selectList(null)) {
                ctx.getCatalogByCode().put(nz(c.getYbCode()), c);
            }
        } catch (Exception ignored) {
            // 目录读取失败不影响其余质控
        }
        try {
            // 目录分表为限价的权威来源
            ctx.setLimitPriceByCode(catalogSplitService.loadLimitPrices());
        } catch (Exception ignored) {
            // ignore
        }
        try {
            for (MisInsuredPerson p : personMapper.selectList(null)) {
                if (!blank(p.getIdCard())) {
                    ctx.getGenderByIdCard().put(p.getIdCard(), p.getGender());
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return ctx;
    }

    /**
     * 对一张结算单的明细清单执行质控。
     *
     * @param bill   结算单
     * @param items  该结算单的费用明细
     * @param rules  启用的质控规则
     */
    public List<Hit> check(MisBill bill, List<MisFeeItem> items, List<MifQualityRule> rules, Ctx ctx) {
        List<Hit> out = new ArrayList<>();
        if (rules == null || items == null) {
            return out;
        }
        for (MifQualityRule r : rules) {
            String type = r.getRuleType() == null ? "" : r.getRuleType();
            switch (type) {
                case "required_field" -> out.addAll(requiredField(r, bill, items, ctx));
                case "not_mapped" -> out.addAll(notMapped(r, bill, items, ctx));
                case "over_limit_price" -> out.addAll(overLimitPrice(r, bill, items, ctx));
                case "invalid_qty" -> out.addAll(invalidQty(r, bill, items));
                case "pay_class_missing" -> out.addAll(payClassMissing(r, bill, items));
                case "duplicate_report" -> out.addAll(duplicateReport(r, bill, items));
                case "amount_mismatch" -> out.addAll(amountMismatch(r, bill, items));
                case "catalog_disabled" -> out.addAll(catalogDisabled(r, bill, items, ctx));
                case "self_ratio_missing" -> out.addAll(selfRatioMissing(r, bill, items, ctx));
                case "gender_limit" -> out.addAll(genderLimit(r, bill, items, ctx));
                default -> {
                    // 未知校验类型跳过
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 校验实现

    /** 必填项缺失：项目名称/医保编码/数量/单价 为空 */
    private List<Hit> notEmpty(List<Hit> out, MifQualityRule r, MisBill bill, MisFeeItem it,
                              String field, String label, String value) {
        if (value == null || value.isBlank()) {
            Hit h = base(r, bill, it);
            h.setFieldName(label);
            h.setOriginValue("");
            h.setQualityDesc("结算清单必填项「" + label + "」为空，质控不通过");
            h.setInvolveAmount(amountOf(it));
            out.add(h);
        }
        return out;
    }

    private List<Hit> requiredField(MifQualityRule r, MisBill bill, List<MisFeeItem> items, Ctx ctx) {
        List<Hit> out = new ArrayList<>();
        for (MisFeeItem it : items) {
            notEmpty(out, r, bill, it, "name", "项目名称", it.getName());
            notEmpty(out, r, bill, it, "ybCode", "医保编码", it.getYbCode());
            if (it.getQty() == null) {
                Hit h = base(r, bill, it);
                h.setFieldName("数量");
                h.setOriginValue("");
                h.setQualityDesc("结算清单必填项「数量」为空，质控不通过");
                h.setInvolveAmount(amountOf(it));
                out.add(h);
            }
            if (it.getPrice() == null) {
                Hit h = base(r, bill, it);
                h.setFieldName("单价");
                h.setOriginValue("");
                h.setQualityDesc("结算清单必填项「单价」为空，质控不通过");
                h.setInvolveAmount(amountOf(it));
                out.add(h);
            }
        }
        return out;
    }

    /** 目录未对照：医保编码为空或目录中查不到 */
    private List<Hit> notMapped(MifQualityRule r, MisBill bill, List<MisFeeItem> items, Ctx ctx) {
        List<Hit> out = new ArrayList<>();
        for (MisFeeItem it : items) {
            if (it.getYbCode() == null || it.getYbCode().isBlank()
                    || !ctx.getCatalogByCode().containsKey(it.getYbCode())) {
                Hit h = base(r, bill, it);
                h.setFieldName("医保编码");
                h.setOriginValue(nz(it.getYbCode()));
                h.setQualityDesc("项目「" + nz(it.getName()) + "」的医保编码「" + nz(it.getYbCode())
                        + "」未在医保目录中完成对照，费用不予结算");
                h.setInvolveAmount(amountOf(it));
                out.add(h);
            }
        }
        return out;
    }

    /** 超限价：单价高于目录限价 */
    private List<Hit> overLimitPrice(MifQualityRule r, MisBill bill, List<MisFeeItem> items, Ctx ctx) {
        List<Hit> out = new ArrayList<>();
        BigDecimal overRatio = param(r, "overRatio", BigDecimal.ZERO);
        for (MisFeeItem it : items) {
            // 限价取目录分表（药品/诊疗/耗材）为权威来源，回落聚合目录
            BigDecimal baseLimit = ctx.getLimitPriceByCode() == null ? null
                    : ctx.getLimitPriceByCode().get(nz(it.getYbCode()));
            if (baseLimit == null) {
                MisCatalog c = ctx.getCatalogByCode().get(nz(it.getYbCode()));
                baseLimit = c == null ? null : c.getLimitPrice();
            }
            if (baseLimit == null || baseLimit.compareTo(BigDecimal.ZERO) <= 0 || it.getPrice() == null) {
                continue;
            }
            BigDecimal limit = baseLimit.multiply(BigDecimal.ONE.add(overRatio));
            if (it.getPrice().compareTo(limit) > 0) {
                BigDecimal qty = it.getQty() == null ? BigDecimal.ONE : BigDecimal.valueOf(it.getQty());
                BigDecimal over = it.getPrice().subtract(baseLimit).multiply(qty);
                Hit h = base(r, bill, it);
                h.setFieldName("单价");
                h.setOriginValue(plain(it.getPrice()));
                h.setQualityDesc(String.format("项目「%s」结算单价 %s 元高于医保限价 %s 元，超限 %s 元",
                        nz(it.getName()), plain(it.getPrice()), plain(baseLimit), plain(over)));
                h.setInvolveAmount(over);
                h.setDeductAmount(deduct(r, over));
                out.add(h);
            }
        }
        return out;
    }

    /** 数量异常：数量非正或超过阈值 */
    private List<Hit> invalidQty(MifQualityRule r, MisBill bill, List<MisFeeItem> items) {
        List<Hit> out = new ArrayList<>();
        int maxQty = paramInt(r, "maxQty", 999);
        for (MisFeeItem it : items) {
            int q = it.getQty() == null ? 0 : it.getQty();
            if (q <= 0) {
                Hit h = base(r, bill, it);
                h.setFieldName("数量");
                h.setOriginValue(String.valueOf(q));
                h.setQualityDesc("项目「" + nz(it.getName()) + "」结算数量为 " + q + "，不符合清单填报规范");
                h.setInvolveAmount(amountOf(it));
                out.add(h);
            } else if (q > maxQty) {
                Hit h = base(r, bill, it);
                h.setFieldName("数量");
                h.setOriginValue(String.valueOf(q));
                h.setQualityDesc("项目「" + nz(it.getName()) + "」结算数量 " + q + " 超过清单阈值 " + maxQty
                        + "，疑存在分解收费或虚记数量");
                h.setInvolveAmount(amountOf(it));
                h.setDeductAmount(deduct(r, amountOf(it)));
                out.add(h);
            }
        }
        return out;
    }

    /** 支付类别缺失 */
    private List<Hit> payClassMissing(MifQualityRule r, MisBill bill, List<MisFeeItem> items) {
        List<Hit> out = new ArrayList<>();
        for (MisFeeItem it : items) {
            if (it.getPayClass() == null || it.getPayClass().isBlank()
                    || !(it.getPayClass().equals("甲") || it.getPayClass().equals("乙") || it.getPayClass().equals("丙"))) {
                Hit h = base(r, bill, it);
                h.setFieldName("支付类别");
                h.setOriginValue(nz(it.getPayClass()));
                h.setQualityDesc("项目「" + nz(it.getName()) + "」未正确填报支付类别（甲/乙/丙），质控不通过");
                h.setInvolveAmount(amountOf(it));
                out.add(h);
            }
        }
        return out;
    }

    /** 重复上报：同一结算单内同项目同金额多次出现 */
    private List<Hit> duplicateReport(MifQualityRule r, MisBill bill, List<MisFeeItem> items) {
        List<Hit> out = new ArrayList<>();
        BigDecimal minAmount = param(r, "minAmount", new BigDecimal("0"));
        Map<String, List<MisFeeItem>> byKey = new LinkedHashMap<>();
        for (MisFeeItem it : items) {
            // 同名同类项目重复出现即为重复上报（不比较单价，单价本身可能不同）
            byKey.computeIfAbsent(nz(it.getName()) + "|" + nz(it.getCategory()),
                    k -> new ArrayList<>()).add(it);
        }
        for (Map.Entry<String, List<MisFeeItem>> e : byKey.entrySet()) {
            List<MisFeeItem> same = e.getValue();
            if (same.size() <= 1) {
                continue;
            }
            BigDecimal sum = BigDecimal.ZERO;
            for (MisFeeItem it : same) {
                sum = sum.add(amountOf(it));
            }
            if (sum.compareTo(minAmount) < 0) {
                continue;
            }
            Hit h = base(r, bill, same.get(0));
            h.setFieldName("重复上报");
            h.setOriginValue("出现 " + same.size() + " 次");
            h.setQualityDesc("项目「" + nz(same.get(0).getName()) + "」在清单中重复上报 " + same.size()
                    + " 次，金额合计 " + plain(sum) + " 元，疑似重复收费");
            h.setInvolveAmount(sum);
            h.setDeductAmount(deduct(r, sum));
            out.add(h);
        }
        return out;
    }

    /** 金额勾稽不一致：明细金额 ≠ 单价 × 数量 */
    private List<Hit> amountMismatch(MifQualityRule r, MisBill bill, List<MisFeeItem> items) {
        List<Hit> out = new ArrayList<>();
        BigDecimal tolerance = param(r, "tolerance", new BigDecimal("0.01"));
        for (MisFeeItem it : items) {
            if (it.getPrice() == null || it.getQty() == null || it.getAmount() == null) {
                continue;
            }
            BigDecimal expect = it.getPrice().multiply(BigDecimal.valueOf(it.getQty()))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal diff = it.getAmount().subtract(expect).abs();
            if (diff.compareTo(tolerance) > 0) {
                Hit h = base(r, bill, it);
                h.setFieldName("金额");
                h.setOriginValue(plain(it.getAmount()));
                h.setQualityDesc(String.format("项目「%s」清单金额 %s 元与单价×数量 %s 元不一致，差异 %s 元",
                        nz(it.getName()), plain(it.getAmount()), plain(expect), plain(diff)));
                h.setInvolveAmount(diff);
                h.setDeductAmount(deduct(r, diff));
                out.add(h);
            }
        }
        return out;
    }

    /** 目录已停用：项目对应的目录条目已停用 */
    private List<Hit> catalogDisabled(MifQualityRule r, MisBill bill, List<MisFeeItem> items, Ctx ctx) {
        List<Hit> out = new ArrayList<>();
        for (MisFeeItem it : items) {
            MisCatalog c = ctx.getCatalogByCode().get(nz(it.getYbCode()));
            if (c == null || c.getStatus() == null || c.getStatus() == 1) {
                continue;
            }
            Hit h = base(r, bill, it);
            h.setFieldName("目录状态");
            h.setOriginValue("停用");
            h.setQualityDesc("项目「" + nz(it.getName()) + "」对应的医保目录条目已停用，不应继续结算");
            h.setInvolveAmount(amountOf(it));
            h.setDeductAmount(deduct(r, amountOf(it)));
            out.add(h);
        }
        return out;
    }

    /** 乙类先行自付比例缺失 */
    private List<Hit> selfRatioMissing(MifQualityRule r, MisBill bill, List<MisFeeItem> items, Ctx ctx) {
        List<Hit> out = new ArrayList<>();
        for (MisFeeItem it : items) {
            if (!"乙".equals(it.getPayClass())) {
                continue;
            }
            if (it.getSelfRatio() == null) {
                MisCatalog c = ctx.getCatalogByCode().get(nz(it.getYbCode()));
                if (c != null && c.getSelfRatio() != null) {
                    continue; // 目录中已维护，可按目录回填
                }
                Hit h = base(r, bill, it);
                h.setFieldName("先行自付比例");
                h.setOriginValue("");
                h.setQualityDesc("乙类项目「" + nz(it.getName()) + "」未维护先行自付比例，无法正确计算个人负担");
                h.setInvolveAmount(amountOf(it));
                out.add(h);
            }
        }
        return out;
    }

    /** 性别限制：目录限定性别的项目与参保人性别不符 */
    private List<Hit> genderLimit(MifQualityRule r, MisBill bill, List<MisFeeItem> items, Ctx ctx) {
        List<Hit> out = new ArrayList<>();
        String expect = paramStr(r, "gender");
        if (expect == null || expect.isBlank()) {
            return out;
        }
        String gender = genderOf(bill, ctx);
        if (gender == null || gender.isBlank()) {
            return out;
        }
        if (!expect.equals(gender)) {
            Hit h = base(r, bill, null);
            h.setFieldName("性别限制");
            h.setOriginValue(gender);
            h.setQualityDesc(String.format("结算清单涉及限定性别为「%s」的项目，但参保人「%s」性别为「%s」，质控不通过",
                    expect, nz(bill.getPatientName()), gender));
            h.setInvolveAmount(nz(bill.getTotalAmount()));
            out.add(h);
        }
        return out;
    }

    // ------------------------------------------------------------------ 工具

    private Hit base(MifQualityRule r, MisBill bill, MisFeeItem it) {
        Hit h = new Hit();
        h.setBillId(bill.getId());
        h.setBillNo(bill.getBillNo());
        h.setRuleId(r.getId());
        h.setRuleCode(r.getRuleCode());
        h.setRuleName(r.getRuleName());
        h.setRuleType(r.getRuleType());
        h.setSeverity(r.getSeverity());
        if (it != null) {
            h.setItemId(it.getId());
            h.setItemName(it.getName());
            h.setYbCode(it.getYbCode());
        }
        return h;
    }

    /** 按规则配置计算建议扣款金额 */
    private BigDecimal deduct(MifQualityRule r, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (r.getDeductType() != null && r.getDeductType() == 2) {
            BigDecimal fixed = r.getDeductAmount() == null ? BigDecimal.ZERO : r.getDeductAmount();
            return base.compareTo(fixed) < 0 ? base : fixed;
        }
        BigDecimal ratio = r.getDeductRatio() == null ? BigDecimal.ZERO : r.getDeductRatio();
        if (ratio.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            ratio = BigDecimal.ONE;
        }
        return base.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    /** 参保人性别：按身份证在参保库中查；查不到则不做该项质控 */
    private String genderOf(MisBill bill, Ctx ctx) {
        if (bill == null || blank(bill.getIdCard())) {
            return null;
        }
        return ctx.getGenderByIdCard().get(bill.getIdCard());
    }

    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private BigDecimal param(MifQualityRule r, String key, BigDecimal def) {
        try {
            Map<?, ?> map = objectMapper.readValue(r.getParams(), Map.class);
            Object v = map.get(key);
            if (v != null) {
                return new BigDecimal(v.toString());
            }
        } catch (Exception ignored) {
            // 参数缺失用默认值
        }
        return def;
    }

    private int paramInt(MifQualityRule r, String key, int def) {
        try {
            Map<?, ?> map = objectMapper.readValue(r.getParams(), Map.class);
            Object v = map.get(key);
            if (v != null) {
                return (int) Double.parseDouble(v.toString());
            }
        } catch (Exception ignored) {
            // 参数缺失用默认值
        }
        return def;
    }

    private String paramStr(MifQualityRule r, String key) {
        try {
            Map<?, ?> map = objectMapper.readValue(r.getParams(), Map.class);
            Object v = map.get(key);
            return v == null ? null : v.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal amountOf(MisFeeItem it) {
        if (it == null) {
            return BigDecimal.ZERO;
        }
        if (it.getAmount() != null) {
            return it.getAmount();
        }
        BigDecimal price = it.getPrice() == null ? BigDecimal.ZERO : it.getPrice();
        BigDecimal qty = it.getQty() == null ? BigDecimal.ONE : BigDecimal.valueOf(it.getQty());
        return price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v;
    }

    private String plain(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
