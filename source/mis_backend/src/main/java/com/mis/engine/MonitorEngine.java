package com.mis.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mis.entity.MisInsuredPerson;
import com.mis.entity.MisSettlement;
import com.mis.entity.MisBill;
import com.mis.entity.MisCatalog;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MisPendingSettle;
import com.mis.entity.MifPrecheckRule;
import com.mis.mapper.MisInsuredPersonMapper;
import com.mis.mapper.MisSettlementMapper;
import com.mis.mapper.MisBillMapper;
import com.mis.mapper.MisCatalogMapper;
import com.mis.mapper.MisFeeItemMapper;
import com.mis.vo.DecVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.mis.service.CatalogSplitService;

/**
 * 智能监控引擎（基金监管「三道防线」前两道）：
 * <ul>
 *   <li>事前提醒（before_settle）：结算前实时比对参保状态、目录限价、重复计费、超量开药等，只提醒不阻断；</li>
 *   <li>事中拦截（in_settle）：结算关口实时判定，命中「阻断」规则直接拒付，命中高风险则转人工复核。</li>
 * </ul>
 * 规则按 mif_precheck_rule.category 分派，阈值从 params（JSON）读取，缺省用内置默认值。
 */
@Component
@RequiredArgsConstructor
public class MonitorEngine {

    private final MisCatalogMapper catalogMapper;
    private final CatalogSplitService catalogSplitService;
    private final MisInsuredPersonMapper personMapper;
    private final MisBillMapper billMapper;
    private final MisSettlementMapper settlementMapper;
    private final MisFeeItemMapper feeItemMapper;
    private final ObjectMapper objectMapper;

    /** 命中明细 */
    @Data
    public static class Hit {
        private Long ruleId;
        private String ruleCode;
        private String ruleName;
        private String triggerStage;
        private Integer riskLevel;
        private Integer blockFlag;
        /** 实际命中的描述（含具体金额/次数/项目） */
        private String promptText;
        private String bizNo;
        private String medinsCode;
        private String medinsName;
        private String insuredIdCard;
        private String insuredName;
        private BigDecimal involveAmount;
    }

    /** 一次评估内复用的历史数据 */
    @Data
    public static class History {
        private List<MisBill> bills = new ArrayList<>();
        private List<MisSettlement> settlements = new ArrayList<>();
        private Map<String, MisCatalog> catalogByCode = new HashMap<>();
        private Map<String, MisInsuredPerson> personByIdCard = new HashMap<>();
        /** 目录分表限价（药品/诊疗/耗材分表为权威来源） */
        private Map<String, BigDecimal> limitPriceByCode = new HashMap<>();
        /** 目录分表支付类别 */
        private Map<String, String> payClassByCode = new HashMap<>();
    }

    // ------------------------------------------------------------------ 对外

    /** 对一笔待结算业务按指定阶段跑规则 */
    public List<Hit> evaluate(List<MifPrecheckRule> rules, MisPendingSettle p, List<MisFeeItem> items, DecVO dec) {
        History h = loadHistory();
        List<Hit> out = new ArrayList<>();
        if (rules == null) {
            return out;
        }
        for (MifPrecheckRule r : rules) {
            out.addAll(evaluateOne(r, p, items, dec, h));
        }
        return out;
    }

    /** 单条规则判定 */
    private List<Hit> evaluateOne(MifPrecheckRule r, MisPendingSettle p, List<MisFeeItem> items, DecVO dec, History h) {
        List<Hit> out = new ArrayList<>();
        String cat = r.getCategory() == null ? "" : r.getCategory().trim();
        switch (cat) {
            case "参保异常" -> out.addAll(notInsured(r, p, h));
            case "冒名" -> out.addAll(identityMismatch(r, p, h));
            case "超限价" -> out.addAll(overLimitPrice(r, items, h));
            case "重复收费" -> out.addAll(duplicateCharge(r, p, items, h));
            case "超量开药" -> out.addAll(overQuantity(r, items));
            case "目录未对照" -> out.addAll(notMapped(r, items, h));
            case "自付占比过高" -> out.addAll(highSelfPayRatio(r, dec));
            case "重复结算" -> out.addAll(duplicateBizNo(r, p, h));
            case "高频就医" -> out.addAll(highFrequency(r, p, h));
            case "分解住院" -> out.addAll(splitInpatient(r, p, h));
            case "同日住院门诊" -> out.addAll(sameDayInOut(r, p, h));
            case "空刷医保卡" -> out.addAll(emptyCard(r, items, dec));
            case "丙类入统筹" -> out.addAll(cClassIntoPool(r, items, dec));
            default -> {
                // 未配置类别的规则不参与判定（仅在规则库中做提示语配置）
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 事前提醒

    /** 参保异常：结算人不在参保库 / 已停保 */
    private List<Hit> notInsured(MifPrecheckRule r, MisPendingSettle p, History h) {
        List<Hit> out = new ArrayList<>();
        if (p == null || blank(p.getIdCard())) {
            return out;
        }
        MisInsuredPerson person = h.getPersonByIdCard().get(p.getIdCard());
        if (person == null) {
            out.add(hit(r, p, "参保人「" + nz(p.getPatientName()) + "」（" + p.getIdCard() + "）未在参保库登记，涉嫌冒名就医",
                    BigDecimal.ZERO));
        } else if (person.getPersonStatus() == null || person.getPersonStatus() != 1) {
            out.add(hit(r, p, "参保人「" + nz(p.getPatientName()) + "」当前为停保状态，不应发生医保结算",
                    BigDecimal.ZERO));
        }
        return out;
    }

    /** 冒名：结算登记的姓名/身份证与参保库登记信息不一致 */
    private List<Hit> identityMismatch(MifPrecheckRule r, MisPendingSettle p, History h) {
        List<Hit> out = new ArrayList<>();
        if (p == null || blank(p.getIdCard())) {
            return out;
        }
        MisInsuredPerson person = h.getPersonByIdCard().get(p.getIdCard());
        if (person == null) {
            return out; // 未登记由「参保异常」规则负责
        }
        if (!blank(person.getName()) && !blank(p.getPatientName()) && !person.getName().equals(p.getPatientName())) {
            out.add(hit(r, p, "结算登记姓名「" + p.getPatientName() + "」与参保库登记姓名「" + person.getName()
                    + "」不一致，疑似冒名就医", BigDecimal.ZERO));
        }
        return out;
    }

    /** 超限价：项目单价高于医保限价 */
    private List<Hit> overLimitPrice(MifPrecheckRule r, List<MisFeeItem> items, History h) {
        List<Hit> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        BigDecimal overRatio = param(r, "overRatio", new BigDecimal("0"));
        BigDecimal overSum = BigDecimal.ZERO;
        StringBuilder detail = new StringBuilder();
        for (MisFeeItem it : items) {
            // 限价取目录分表（药品/诊疗/耗材）为权威来源，回落聚合目录
            BigDecimal baseLimit = h.getLimitPriceByCode() == null ? null
                    : h.getLimitPriceByCode().get(nz(it.getYbCode()));
            if (baseLimit == null) {
                MisCatalog c = h.getCatalogByCode().get(nz(it.getYbCode()));
                baseLimit = c == null ? null : c.getLimitPrice();
            }
            if (baseLimit == null || baseLimit.compareTo(BigDecimal.ZERO) <= 0 || it.getPrice() == null) {
                continue;
            }
            BigDecimal limit = baseLimit.multiply(BigDecimal.ONE.add(overRatio));
            if (it.getPrice().compareTo(limit) > 0) {
                BigDecimal qty = it.getQty() == null ? BigDecimal.ONE : BigDecimal.valueOf(it.getQty());
                overSum = overSum.add(it.getPrice().subtract(baseLimit).multiply(qty));
                if (detail.length() > 0) {
                    detail.append("；");
                }
                detail.append("「").append(nz(it.getName())).append("」单价 ").append(plain(it.getPrice()))
                        .append(" 元 > 医保限价 ").append(plain(baseLimit)).append(" 元");
            }
        }
        if (overSum.compareTo(BigDecimal.ZERO) > 0) {
            Hit h2 = hit(r, null, "存在超限价收费：" + detail + "，超限金额合计 " + plain(overSum) + " 元", overSum);
            out.add(h2);
        }
        return out;
    }

    /** 重复收费：同一结算单内同名同类项目多次计费 */
    private List<Hit> duplicateCharge(MifPrecheckRule r, MisPendingSettle p, List<MisFeeItem> items, History h) {
        List<Hit> out = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return out;
        }
        BigDecimal minAmount = param(r, "minAmount", new BigDecimal("100"));
        Map<String, List<MisFeeItem>> byName = new LinkedHashMap<>();
        for (MisFeeItem it : items) {
            byName.computeIfAbsent(nz(it.getName()) + "|" + nz(it.getCategory()), k -> new ArrayList<>()).add(it);
        }
        for (List<MisFeeItem> same : byName.values()) {
            if (same.size() <= 1) {
                continue;
            }
            BigDecimal sum = BigDecimal.ZERO;
            for (MisFeeItem it : same) {
                sum = sum.add(amountOf(it));
            }
            if (sum.compareTo(minAmount) >= 0) {
                out.add(hit(r, p, "同一结算单中项目「" + nz(same.get(0).getName()) + "」重复计费 " + same.size()
                        + " 次，合计 " + plain(sum) + " 元，疑似重复收费", sum));
            }
        }
        return out;
    }

    /** 超量开药：同一项目数量超过配置阈值 */
    private List<Hit> overQuantity(MifPrecheckRule r, List<MisFeeItem> items) {
        List<Hit> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        int maxQty = paramInt(r, "maxQty", 10);
        Map<String, Integer> qtyByName = new LinkedHashMap<>();
        for (MisFeeItem it : items) {
            String key = nz(it.getName());
            int q = it.getQty() == null ? 1 : it.getQty();
            qtyByName.merge(key, q, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : qtyByName.entrySet()) {
            if (e.getValue() > maxQty) {
                out.add(hit(r, null, "项目「" + e.getKey() + "」本次开量 " + e.getValue()
                        + "，超过单次阈值 " + maxQty + "，涉嫌超量开药", BigDecimal.ZERO));
            }
        }
        return out;
    }

    /** 目录未对照：项目无医保编码或目录中查不到 */
    private List<Hit> notMapped(MifPrecheckRule r, List<MisFeeItem> items, History h) {
        List<Hit> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        List<String> names = new ArrayList<>();
        for (MisFeeItem it : items) {
            if (blank(it.getYbCode()) || !h.getCatalogByCode().containsKey(nz(it.getYbCode()))) {
                names.add(nz(it.getName()));
            }
        }
        if (!names.isEmpty()) {
            out.add(hit(r, null, "以下项目未完成医保目录对照：" + String.join("、", names.subList(0, Math.min(5, names.size())))
                    + (names.size() > 5 ? " 等 " + names.size() + " 项" : ""), BigDecimal.ZERO));
        }
        return out;
    }

    /** 自付占比过高：个人自付占总费用比例超阈值 */
    private List<Hit> highSelfPayRatio(MifPrecheckRule r, DecVO dec) {
        List<Hit> out = new ArrayList<>();
        if (dec == null || dec.getTotal() == null || dec.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return out;
        }
        BigDecimal ratio = param(r, "selfPayRatio", new BigDecimal("0.5"));
        BigDecimal real = nz(dec.getSelfPay()).divide(dec.getTotal(), 4, RoundingMode.HALF_UP);
        if (real.compareTo(ratio) > 0) {
            out.add(hit(r, null, String.format("个人自付 %s 元占费用总额 %s 元的 %s%%，超过 %s%% 阈值，请确认是否合规",
                    plain(dec.getSelfPay()), plain(dec.getTotal()),
                    real.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString(),
                    ratio.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString()),
                    nz(dec.getSelfPay())));
        }
        return out;
    }

    // ------------------------------------------------------------------ 事中拦截

    /** 重复结算：同一业务单号已存在有效结算单 */
    private List<Hit> duplicateBizNo(MifPrecheckRule r, MisPendingSettle p, History h) {
        List<Hit> out = new ArrayList<>();
        if (p == null || blank(p.getBizNo())) {
            return out;
        }
        for (MisBill b : h.getBills()) {
            if (p.getBizNo().equals(nz(b.getBizNo())) && !"已退费".equals(b.getStatus())) {
                out.add(hit(r, p, "业务单号 " + p.getBizNo() + " 已存在结算单 " + nz(b.getBillNo())
                        + "，疑似重复结算", nz(b.getInsurancePay())));
                break;
            }
        }
        return out;
    }

    /** 高频就医：窗口期内同一参保人结算次数超阈值 */
    private List<Hit> highFrequency(MifPrecheckRule r, MisPendingSettle p, History h) {
        List<Hit> out = new ArrayList<>();
        if (p == null || blank(p.getIdCard())) {
            return out;
        }
        int windowDays = paramInt(r, "windowDays", 7);
        int threshold = paramInt(r, "threshold", 5);
        LocalDate today = p.getBizTime() == null ? LocalDate.now() : p.getBizTime().toLocalDate();
        int count = 1;
        BigDecimal amount = BigDecimal.ZERO;
        for (MisBill b : h.getBills()) {
            if (!p.getIdCard().equals(nz(b.getIdCard())) || "已退费".equals(b.getStatus())) {
                continue;
            }
            LocalDate d = b.getSettleTime() == null ? null : b.getSettleTime().toLocalDate();
            if (d != null && ChronoUnit.DAYS.between(d, today) >= 0 && ChronoUnit.DAYS.between(d, today) <= windowDays) {
                count++;
                amount = amount.add(nz(b.getTotalAmount()));
            }
        }
        if (count > threshold) {
            out.add(hit(r, p, String.format("参保人「%s」近 %d 天已结算 %d 次，超过阈值 %d 次，涉嫌高频就医/空刷",
                    nz(p.getPatientName()), windowDays, count, threshold), amount));
        }
        return out;
    }

    /** 分解住院：窗口期内同一参保人住院结算次数超阈值 */
    private List<Hit> splitInpatient(MifPrecheckRule r, MisPendingSettle p, History h) {
        List<Hit> out = new ArrayList<>();
        if (p == null || blank(p.getIdCard()) || p.getBizType() == null || p.getBizType() != 2) {
            return out;
        }
        int windowDays = paramInt(r, "windowDays", 30);
        int minCount = paramInt(r, "minCount", 2);
        LocalDate today = p.getBizTime() == null ? LocalDate.now() : p.getBizTime().toLocalDate();
        List<LocalDate> dates = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO;
        for (MisBill b : h.getBills()) {
            if (!p.getIdCard().equals(nz(b.getIdCard())) || !"住院".equals(b.getSettleType()) || "已退费".equals(b.getStatus())) {
                continue;
            }
            LocalDate d = b.getSettleTime() == null ? null : b.getSettleTime().toLocalDate();
            if (d != null && ChronoUnit.DAYS.between(d, today) >= 0 && ChronoUnit.DAYS.between(d, today) <= windowDays) {
                dates.add(d);
                amount = amount.add(nz(b.getTotalAmount()));
            }
        }
        if (dates.size() + 1 >= minCount) {
            out.add(hit(r, p, String.format("参保人「%s」近 %d 天内住院结算 %d 次，疑似分解住院，涉及费用 %s 元",
                    nz(p.getPatientName()), windowDays, dates.size() + 1, plain(amount)), amount));
        }
        return out;
    }

    /** 同日住院门诊：同一天既有住院又有门诊结算 */
    private List<Hit> sameDayInOut(MifPrecheckRule r, MisPendingSettle p, History h) {
        List<Hit> out = new ArrayList<>();
        if (p == null || blank(p.getIdCard()) || p.getBizTime() == null) {
            return out;
        }
        LocalDate today = p.getBizTime().toLocalDate();
        String other = p.getBizType() != null && p.getBizType() == 2 ? "门诊" : "住院";
        for (MisBill b : h.getBills()) {
            if (!p.getIdCard().equals(nz(b.getIdCard())) || "已退费".equals(b.getStatus())) {
                continue;
            }
            LocalDate d = b.getSettleTime() == null ? null : b.getSettleTime().toLocalDate();
            if (today.equals(d) && other.equals(b.getSettleType())) {
                out.add(hit(r, p, "参保人「" + nz(p.getPatientName()) + "」于 " + today + " 同时存在"
                        + other + "结算单 " + nz(b.getBillNo()) + "，疑似重复就医/冒名", nz(b.getTotalAmount())));
                break;
            }
        }
        return out;
    }

    /** 空刷医保卡：有项目但费用几乎为零 */
    private List<Hit> emptyCard(MifPrecheckRule r, List<MisFeeItem> items, DecVO dec) {
        List<Hit> out = new ArrayList<>();
        if (items == null || items.isEmpty() || dec == null) {
            return out;
        }
        BigDecimal minAmount = param(r, "minAmount", new BigDecimal("1"));
        if (nz(dec.getTotal()).compareTo(minAmount) <= 0) {
            out.add(hit(r, null, "本笔结算共 " + items.size() + " 个项目但费用合计仅 " + plain(dec.getTotal())
                    + " 元，疑似空刷医保卡", BigDecimal.ZERO));
        }
        return out;
    }

    /** 丙类项目入统筹：丙类金额超阈值且发生统筹支付 */
    private List<Hit> cClassIntoPool(MifPrecheckRule r, List<MisFeeItem> items, DecVO dec) {
        List<Hit> out = new ArrayList<>();
        if (items == null || dec == null) {
            return out;
        }
        if (nz(dec.getInsurancePay()).compareTo(BigDecimal.ZERO) <= 0) {
            return out;
        }
        BigDecimal minAmount = param(r, "minAmount", new BigDecimal("200"));
        BigDecimal cSum = BigDecimal.ZERO;
        for (MisFeeItem it : items) {
            if ("丙".equals(it.getPayClass())) {
                cSum = cSum.add(amountOf(it));
            }
        }
        if (cSum.compareTo(minAmount) >= 0) {
            out.add(hit(r, null, "本笔结算含丙类（自费）项目 " + plain(cSum) + " 元且仍发生统筹支付 "
                    + plain(dec.getInsurancePay()) + " 元，涉嫌串换项目/虚记费用", cSum));
        }
        return out;
    }

    // ------------------------------------------------------------------ 工具

    private History loadHistory() {
        History h = new History();
        try {
            h.setBills(billMapper.selectList(null));
        } catch (Exception ignored) {
            // 历史数据读取失败不影响事前提醒
        }
        try {
            h.setSettlements(settlementMapper.selectList(null));
        } catch (Exception ignored) {
            // ignore
        }
        try {
            for (MisCatalog c : catalogMapper.selectList(null)) {
                h.getCatalogByCode().put(nz(c.getYbCode()), c);
            }
        } catch (Exception ignored) {
            // ignore
        }
        try {
            // 目录分表（药品/诊疗/耗材）为限价与支付类别的权威来源
            h.setLimitPriceByCode(catalogSplitService.loadLimitPrices());
            h.setPayClassByCode(catalogSplitService.loadPayClasses());
        } catch (Exception ignored) {
            // ignore
        }
        try {
            for (MisInsuredPerson p : personMapper.selectList(null)) {
                if (!blank(p.getIdCard())) {
                    h.getPersonByIdCard().put(p.getIdCard(), p);
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return h;
    }

    private Hit hit(MifPrecheckRule r, MisPendingSettle p, String text, BigDecimal amount) {
        Hit h = new Hit();
        h.setRuleId(r.getId());
        h.setRuleCode(r.getRuleCode());
        h.setRuleName(r.getRuleName());
        h.setTriggerStage(r.getTriggerStage());
        h.setRiskLevel(r.getRiskLevel());
        h.setBlockFlag(r.getBlockFlag());
        h.setPromptText(text);
        h.setInvolveAmount(amount);
        if (p != null) {
            h.setBizNo(p.getBizNo());
            h.setInsuredIdCard(p.getIdCard());
            h.setInsuredName(p.getPatientName());
        }
        return h;
    }

    private BigDecimal param(MifPrecheckRule r, String key, BigDecimal def) {
        try {
            Map<?, ?> map = objectMapper.readValue(r.getParams(), Map.class);
            Object v = map.get(key);
            if (v != null) {
                return new BigDecimal(v.toString());
            }
        } catch (Exception ignored) {
            // 参数缺失/格式错用默认值
        }
        return def;
    }

    private int paramInt(MifPrecheckRule r, String key, int def) {
        try {
            Map<?, ?> map = objectMapper.readValue(r.getParams(), Map.class);
            Object v = map.get(key);
            if (v != null) {
                return (int) Double.parseDouble(v.toString());
            }
        } catch (Exception ignored) {
            // 参数缺失/格式错用默认值
        }
        return def;
    }

    private BigDecimal amountOf(MisFeeItem it) {
        if (it.getAmount() != null) {
            return it.getAmount();
        }
        BigDecimal price = it.getPrice() == null ? BigDecimal.ZERO : it.getPrice();
        BigDecimal qty = it.getQty() == null ? BigDecimal.ONE : BigDecimal.valueOf(it.getQty());
        return price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String plain(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
