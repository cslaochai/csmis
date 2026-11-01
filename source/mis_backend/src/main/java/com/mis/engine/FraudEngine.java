package com.mis.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mis.entity.MisSettlement;
import com.mis.entity.MisAgreement;
import com.mis.entity.MisBill;
import com.mis.entity.MisCatalog;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MifFraudModel;
import com.mis.mapper.MisSettlementMapper;
import com.mis.mapper.MisAgreementMapper;
import com.mis.mapper.MisBillMapper;
import com.mis.mapper.MisCatalogMapper;
import com.mis.mapper.MisFeeItemMapper;
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

/**
 * 大数据反欺诈引擎：按模型类别对真实业务数据做关联/趋势分析，产出反欺诈线索。
 * <p>模型参数从 mif_fraud_model.params（JSON）读取，缺省用内置默认值。
 * 数据源：经办结算单（mis_bill + mis_fee_item）与机构报盘（mis_settlement）。
 */
@Component
@RequiredArgsConstructor
public class FraudEngine {

    private final MisBillMapper billMapper;
    private final MisFeeItemMapper feeItemMapper;
    private final MisSettlementMapper settlementMapper;
    private final MisAgreementMapper agreementMapper;
    private final MisCatalogMapper catalogMapper;
    private final ObjectMapper objectMapper;

    /** 反欺诈线索候选 */
    @Data
    public static class Clue {
        private Long modelId;
        private String modelCode;
        private String modelName;
        private String medinsCode;
        private String medinsName;
        private String insuredIdCard;
        private String insuredName;
        private String bizNo;
        private BigDecimal involveAmount;
        private Integer riskLevel;
        private String clueDetail;
    }

    /** 一次运行内复用的数据 */
    @Data
    public static class Ctx {
        private List<MisBill> bills = new ArrayList<>();
        private List<MisSettlement> settlements = new ArrayList<>();
        private Map<Long, List<MisFeeItem>> itemsByBill = new HashMap<>();
        private Map<String, MisAgreement> agreementByCode = new HashMap<>();
        private Map<String, MisCatalog> catalogByCode = new HashMap<>();
    }

    // ------------------------------------------------------------------ 入口

    public List<Clue> run(MifFraudModel model) {
        Ctx d = loadData();
        String cat = model.getCategory() == null ? "" : model.getCategory().trim();
        List<Clue> out = new ArrayList<>();
        switch (cat) {
            case "01" -> out.addAll(splitHospitalization(model, d));
            case "02" -> out.addAll(fakeVisit(model, d));
            case "03" -> out.addAll(drugSwitch(model, d));
            case "04" -> out.addAll(overTreatment(model, d));
            case "05" -> out.addAll(emptyCard(model, d));
            case "06" -> out.addAll(other(model, d));
            default -> {
                // 未知类别不产出线索
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 01 分解住院

    /**
     * 分解住院：同一参保人窗口期内住院结算次数超阈值，或多次住院且单次费用畸低。
     */
    private List<Clue> splitHospitalization(MifFraudModel model, Ctx d) {
        List<Clue> out = new ArrayList<>();
        int windowDays = paramInt(model, "windowDays", 30);
        int maxAdm = paramInt(model, "maxAdm", 3);
        BigDecimal minAmount = param(model, "minAmount", new BigDecimal("800"));

        Map<String, List<MisBill>> byPerson = new HashMap<>();
        for (MisBill b : d.getBills()) {
            if (!"住院".equals(b.getSettleType()) || "已退费".equals(b.getStatus()) || blank(b.getIdCard())) {
                continue;
            }
            byPerson.computeIfAbsent(b.getIdCard(), k -> new ArrayList<>()).add(b);
        }
        for (Map.Entry<String, List<MisBill>> e : byPerson.entrySet()) {
            List<MisBill> list = e.getValue();
            list.sort((a, b) -> nzDate(a.getSettleTime()).compareTo(nzDate(b.getSettleTime())));
            // 滑动窗口统计
            for (int i = 0; i < list.size(); i++) {
                LocalDate from = nzDate(list.get(i).getSettleTime()).toLocalDate();
                List<MisBill> win = new ArrayList<>();
                for (int j = i; j < list.size(); j++) {
                    LocalDate to = nzDate(list.get(j).getSettleTime()).toLocalDate();
                    if (ChronoUnit.DAYS.between(from, to) > windowDays) {
                        break;
                    }
                    win.add(list.get(j));
                }
                if (win.size() < 2) {
                    continue;
                }
                BigDecimal sum = BigDecimal.ZERO;
                boolean allLow = true;
                for (MisBill b : win) {
                    sum = sum.add(nz(b.getTotalAmount()));
                    if (nz(b.getTotalAmount()).compareTo(minAmount) >= 0) {
                        allLow = false;
                    }
                }
                boolean tooMany = win.size() > maxAdm;
                boolean lowCostSplit = allLow && win.size() >= 2;
                if (!tooMany && !lowCostSplit) {
                    continue;
                }
                MisBill first = win.get(0);
                Clue c = base(model, first.getPatientName(), first.getIdCard(), first.getBillNo(), sum);
                StringBuilder dates = new StringBuilder();
                for (MisBill b : win) {
                    if (dates.length() > 0) {
                        dates.append("、");
                    }
                    dates.append(nzDate(b.getSettleTime()).toLocalDate());
                }
                c.setClueDetail(String.format(
                        "参保人「%s」在 %d 天内（%s）住院结算 %d 次，涉及费用 %s 元%s，疑似分解住院",
                        nz(first.getPatientName()), windowDays, dates, win.size(), plain(sum),
                        tooMany ? "，住院次数超过阈值 " + maxAdm : "，单次住院费用均低于 " + plain(minAmount) + " 元"));
                out.add(c);
                break; // 同一参保人只出一条线索
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 02 虚假就医/冒名

    /**
     * 虚假就医：同一参保人窗口期内结算次数超阈值；或同一日内在多个科室/机构重复结算。
     */
    private List<Clue> fakeVisit(MifFraudModel model, Ctx d) {
        List<Clue> out = new ArrayList<>();
        int windowDays = paramInt(model, "windowDays", 7);
        int threshold = paramInt(model, "threshold", 5);

        Map<String, List<MisBill>> byPerson = new HashMap<>();
        for (MisBill b : d.getBills()) {
            if ("已退费".equals(b.getStatus()) || blank(b.getIdCard())) {
                continue;
            }
            byPerson.computeIfAbsent(b.getIdCard(), k -> new ArrayList<>()).add(b);
        }
        for (Map.Entry<String, List<MisBill>> e : byPerson.entrySet()) {
            List<MisBill> list = e.getValue();
            if (list.size() <= threshold) {
                continue;
            }
            LocalDate latest = list.stream().map(b -> nzDate(b.getSettleTime()).toLocalDate()).max(LocalDate::compareTo).orElse(LocalDate.now());
            List<MisBill> win = list.stream()
                    .filter(b -> ChronoUnit.DAYS.between(nzDate(b.getSettleTime()).toLocalDate(), latest) <= windowDays)
                    .toList();
            if (win.size() <= threshold) {
                continue;
            }
            BigDecimal sum = BigDecimal.ZERO;
            for (MisBill b : win) {
                sum = sum.add(nz(b.getTotalAmount()));
            }
            MisBill first = win.get(0);
            Clue c = base(model, first.getPatientName(), first.getIdCard(), first.getBillNo(), sum);
            c.setClueDetail(String.format("参保人「%s」近 %d 天内结算 %d 次（阈值 %d 次），就诊频次显著偏高，疑似冒名就医或空刷",
                    nz(first.getPatientName()), windowDays, win.size(), threshold));
            out.add(c);
        }

        // 同一身份证在机构报盘中同日于多机构结算（跨机构冒名的典型特征）
        Map<String, List<MisSettlement>> stByPerson = new HashMap<>();
        for (MisSettlement s : d.getSettlements()) {
            if (s.getStatus() != null && s.getStatus() == 2 || blank(s.getIdCard())) {
                continue;
            }
            stByPerson.computeIfAbsent(s.getIdCard(), k -> new ArrayList<>()).add(s);
        }
        for (Map.Entry<String, List<MisSettlement>> e : stByPerson.entrySet()) {
            Map<LocalDate, List<MisSettlement>> byDay = new LinkedHashMap<>();
            for (MisSettlement s : e.getValue()) {
                byDay.computeIfAbsent(s.getBillDate() == null ? LocalDate.now() : s.getBillDate(), k -> new ArrayList<>()).add(s);
            }
            for (Map.Entry<LocalDate, List<MisSettlement>> day : byDay.entrySet()) {
                java.util.Set<String> medins = new java.util.HashSet<>();
                for (MisSettlement s : day.getValue()) {
                    medins.add(nz(s.getFixMedinsCode()));
                }
                if (medins.size() < 2) {
                    continue;
                }
                MisSettlement first = day.getValue().get(0);
                BigDecimal sum = BigDecimal.ZERO;
                for (MisSettlement s : day.getValue()) {
                    sum = sum.add(nz(s.getTotalAmount()));
                }
                Clue c = base(model, first.getPatientName(), first.getIdCard(), first.getTradeNo(), sum);
                c.setMedinsCode(nz(first.getFixMedinsCode()));
                c.setMedinsName(medinsName(d, nz(first.getFixMedinsCode())));
                c.setClueDetail(String.format("参保人「%s」于 %s 同日在 %d 家定点机构（%s）发生结算，疑似冒名就医",
                        nz(first.getPatientName()), day.getKey(), medins.size(), String.join("、", medins)));
                out.add(c);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 03 串换药品

    /**
     * 串换药品：同一结算单内同一药品名称对应多个医保编码，或存在未对照目录的项目。
     */
    private List<Clue> drugSwitch(MifFraudModel model, Ctx d) {
        List<Clue> out = new ArrayList<>();
        BigDecimal matchRate = param(model, "matchRate", new BigDecimal("0.85"));
        for (MisBill b : d.getBills()) {
            if ("已退费".equals(b.getStatus())) {
                continue;
            }
            List<MisFeeItem> items = d.getItemsByBill().getOrDefault(b.getId(), List.of());
            if (items.isEmpty()) {
                continue;
            }
            // 同一药品名称多个医保编码
            Map<String, java.util.Set<String>> codeByName = new LinkedHashMap<>();
            BigDecimal unmappedAmount = BigDecimal.ZERO;
            for (MisFeeItem it : items) {
                codeByName.computeIfAbsent(nz(it.getName()), k -> new java.util.LinkedHashSet<>()).add(nz(it.getYbCode()));
                if (blank(it.getYbCode()) || !d.getCatalogByCode().containsKey(nz(it.getYbCode()))) {
                    unmappedAmount = unmappedAmount.add(nz(it.getAmount()));
                }
            }
            List<String> multiCoded = new ArrayList<>();
            for (Map.Entry<String, java.util.Set<String>> e : codeByName.entrySet()) {
                if (e.getValue().size() > 1) {
                    multiCoded.add(e.getKey() + "→" + String.join("/", e.getValue()));
                }
            }
            BigDecimal total = nz(b.getTotalAmount());
            double unmappedRatio = total.compareTo(BigDecimal.ZERO) > 0
                    ? unmappedAmount.divide(total, 4, RoundingMode.HALF_UP).doubleValue() : 0d;
            if (multiCoded.isEmpty() && unmappedRatio <= 1 - matchRate.doubleValue()) {
                continue;
            }
            Clue c = base(model, b.getPatientName(), b.getIdCard(), b.getBillNo(), unmappedAmount);
            StringBuilder sb = new StringBuilder();
            if (!multiCoded.isEmpty()) {
                sb.append("同一结算单内药品「").append(String.join("、", multiCoded)).append("」对应多个医保编码");
            }
            if (unmappedAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (sb.length() > 0) {
                    sb.append("；");
                }
                sb.append("未对照医保目录的项目金额 ").append(plain(unmappedAmount)).append(" 元，占比 ")
                        .append(new BigDecimal(unmappedRatio * 100).setScale(1, RoundingMode.HALF_UP)).append("%");
            }
            sb.append("，疑似串换药品/项目");
            c.setClueDetail(sb.toString());
            out.add(c);
        }
        return out;
    }

    // ------------------------------------------------------------------ 04 过度医疗

    /**
     * 过度医疗：住院费用显著高于同险种均值，或费用中自费（丙类）占比畸高。
     */
    private List<Clue> overTreatment(MifFraudModel model, Ctx d) {
        List<Clue> out = new ArrayList<>();
        BigDecimal multiple = param(model, "multiple", new BigDecimal("1.8"));
        BigDecimal cRatioLimit = param(model, "cRatio", new BigDecimal("0.5"));

        Map<String, List<MisBill>> inpatientByType = new HashMap<>();
        for (MisBill b : d.getBills()) {
            if ("已退费".equals(b.getStatus())) {
                continue;
            }
            inpatientByType.computeIfAbsent(nz(b.getInsuranceType()), k -> new ArrayList<>()).add(b);
        }
        Map<String, BigDecimal> avgByType = new HashMap<>();
        for (Map.Entry<String, List<MisBill>> e : inpatientByType.entrySet()) {
            BigDecimal sum = BigDecimal.ZERO;
            int n = 0;
            for (MisBill b : e.getValue()) {
                if ("住院".equals(b.getSettleType())) {
                    sum = sum.add(nz(b.getTotalAmount()));
                    n++;
                }
            }
            if (n > 0) {
                avgByType.put(e.getKey(), sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP));
            }
        }
        for (MisBill b : d.getBills()) {
            if ("已退费".equals(b.getStatus())) {
                continue;
            }
            List<MisFeeItem> items = d.getItemsByBill().getOrDefault(b.getId(), List.of());
            BigDecimal cSum = BigDecimal.ZERO;
            for (MisFeeItem it : items) {
                if ("丙".equals(it.getPayClass())) {
                    cSum = cSum.add(nz(it.getAmount()));
                }
            }
            BigDecimal total = nz(b.getTotalAmount());
            BigDecimal avg = avgByType.get(nz(b.getInsuranceType()));
            boolean highCost = "住院".equals(b.getSettleType()) && avg != null
                    && avg.compareTo(BigDecimal.ZERO) > 0
                    && total.compareTo(avg.multiply(multiple)) > 0;
            double cRatio = total.compareTo(BigDecimal.ZERO) > 0
                    ? cSum.divide(total, 4, RoundingMode.HALF_UP).doubleValue() : 0d;
            boolean highCRatio = cSum.compareTo(BigDecimal.ZERO) > 0 && new BigDecimal(cRatio).compareTo(cRatioLimit) > 0;
            if (!highCost && !highCRatio) {
                continue;
            }
            Clue c = base(model, b.getPatientName(), b.getIdCard(), b.getBillNo(), total);
            StringBuilder sb = new StringBuilder();
            if (highCost) {
                sb.append(String.format("%s结算费用 %s 元，高于%s险种住院均值 %s 元的 %s 倍",
                        b.getSettleType(), plain(total), nz(b.getInsuranceType()), plain(avg),
                        multiple.stripTrailingZeros().toPlainString()));
            }
            if (highCRatio) {
                if (sb.length() > 0) {
                    sb.append("；");
                }
                sb.append(String.format("自费（丙类）项目 %s 元，占总费用 %s%%",
                        plain(cSum), new BigDecimal(cRatio * 100).setScale(1, RoundingMode.HALF_UP)));
            }
            sb.append("，疑似过度医疗");
            c.setClueDetail(sb.toString());
            out.add(c);
        }
        return out;
    }

    // ------------------------------------------------------------------ 05 空刷医保卡

    /**
     * 空刷医保卡：结算单有费用明细但费用畸低/统筹为零，或结算单无任何费用明细。
     */
    private List<Clue> emptyCard(MifFraudModel model, Ctx d) {
        List<Clue> out = new ArrayList<>();
        BigDecimal minAmount = param(model, "minAmount", new BigDecimal("1"));
        for (MisBill b : d.getBills()) {
            if ("已退费".equals(b.getStatus())) {
                continue;
            }
            List<MisFeeItem> items = d.getItemsByBill().getOrDefault(b.getId(), List.of());
            if (items.isEmpty()) {
                Clue c = base(model, b.getPatientName(), b.getIdCard(), b.getBillNo(), BigDecimal.ZERO);
                c.setClueDetail("结算单 " + nz(b.getBillNo()) + " 无任何费用明细，疑似空刷医保卡/虚构交易");
                out.add(c);
                continue;
            }
            BigDecimal total = nz(b.getTotalAmount());
            if (total.compareTo(minAmount) <= 0 || nz(b.getInsurancePay()).compareTo(BigDecimal.ZERO) <= 0) {
                Clue c = base(model, b.getPatientName(), b.getIdCard(), b.getBillNo(), total);
                c.setClueDetail(String.format("结算单 %s 共 %d 个项目但费用合计仅 %s 元、统筹支付 %s 元，疑似空刷医保卡",
                        nz(b.getBillNo()), items.size(), plain(total), plain(b.getInsurancePay())));
                out.add(c);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 06 其他

    /**
     * 其他：数据一致性异常——结算单已退费但费用明细未冲正，存在重复结算/资金回退风险。
     */
    private List<Clue> other(MifFraudModel model, Ctx d) {
        List<Clue> out = new ArrayList<>();
        for (MisBill b : d.getBills()) {
            if (!"已退费".equals(b.getStatus())) {
                continue;
            }
            List<MisFeeItem> items = d.getItemsByBill().getOrDefault(b.getId(), List.of());
            long notReversed = items.stream().filter(it -> !Boolean.TRUE.equals(it.getReversed())).count();
            if (notReversed == 0) {
                continue;
            }
            Clue c = base(model, b.getPatientName(), b.getIdCard(), b.getBillNo(), nz(b.getInsurancePay()));
            c.setClueDetail("结算单 " + nz(b.getBillNo()) + " 已退费，但仍有 " + notReversed + " 条费用明细未冲正，存在资金回退与重复结算风险");
            out.add(c);
        }
        return out;
    }

    // ------------------------------------------------------------------ 工具

    private Ctx loadData() {
        Ctx d = new Ctx();
        d.setBills(billMapper.selectList(null));
        d.setSettlements(settlementMapper.selectList(null));
        for (MisFeeItem it : feeItemMapper.selectList(null)) {
            if (it.getOwnerType() != null && it.getOwnerType() == 2) {
                d.getItemsByBill().computeIfAbsent(it.getOwnerId(), k -> new ArrayList<>()).add(it);
            }
        }
        for (MisAgreement a : agreementMapper.selectList(null)) {
            d.getAgreementByCode().put(nz(a.getMedinsCode()), a);
        }
        for (MisCatalog c : catalogMapper.selectList(null)) {
            d.getCatalogByCode().put(nz(c.getYbCode()), c);
        }
        return d;
    }

    private Clue base(MifFraudModel model, String insuredName, String idCard, String bizNo, BigDecimal amount) {
        Clue c = new Clue();
        c.setModelId(model.getId());
        c.setModelCode(model.getModelCode());
        c.setModelName(model.getModelName());
        c.setInsuredName(insuredName);
        c.setInsuredIdCard(idCard);
        c.setBizNo(bizNo);
        c.setInvolveAmount(amount);
        c.setRiskLevel(model.getRiskLevel());
        return c;
    }

    private String medinsName(Ctx d, String medinsCode) {
        MisAgreement a = d.getAgreementByCode().get(medinsCode);
        return a == null ? null : a.getMedinsName();
    }

    private BigDecimal param(MifFraudModel model, String key, BigDecimal def) {
        try {
            Map<?, ?> map = objectMapper.readValue(model.getParams(), Map.class);
            Object v = map.get(key);
            if (v != null) {
                return new BigDecimal(v.toString());
            }
        } catch (Exception ignored) {
            // 参数缺失/格式错用默认值
        }
        return def;
    }

    private int paramInt(MifFraudModel model, String key, int def) {
        try {
            Map<?, ?> map = objectMapper.readValue(model.getParams(), Map.class);
            Object v = map.get(key);
            if (v != null) {
                return (int) Double.parseDouble(v.toString());
            }
        } catch (Exception ignored) {
            // 参数缺失/格式错用默认值
        }
        return def;
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

    private java.time.LocalDateTime nzDate(java.time.LocalDateTime t) {
        return t == null ? java.time.LocalDateTime.now() : t;
    }

    private String plain(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
