package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mis.entity.MisInsuredPerson;
import com.mis.entity.MisMonthly;
import com.mis.entity.MisSettlement;
import com.mis.entity.MisAgreement;
import com.mis.entity.MisBill;
import com.mis.entity.MisDailyAlloc;
import com.mis.entity.MisFeeAudit;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MifFraudClue;
import com.mis.entity.MifPrecheckResult;
import com.mis.entity.MifQualityResult;
import com.mis.entity.MifRecover;
import com.mis.entity.MifSelfCheck;
import com.mis.entity.MifSuspicion;
import com.mis.entity.MifTraceVerifyLog;
import com.mis.mapper.MisInsuredPersonMapper;
import com.mis.mapper.MisMonthlyMapper;
import com.mis.mapper.MisSettlementMapper;
import com.mis.mapper.MisAgreementMapper;
import com.mis.mapper.MisBillMapper;
import com.mis.mapper.MisDailyAllocMapper;
import com.mis.mapper.MisFeeAuditMapper;
import com.mis.mapper.MisFeeItemMapper;
import com.mis.mapper.MifFraudClueMapper;
import com.mis.mapper.MifPrecheckResultMapper;
import com.mis.mapper.MifQualityResultMapper;
import com.mis.mapper.MifRecoverMapper;
import com.mis.mapper.MifSelfCheckMapper;
import com.mis.mapper.MifSuspicionMapper;
import com.mis.mapper.MifTraceVerifyLogMapper;
import com.mis.service.FundAnalysisService;
import com.mis.vo.DimensionStatVO;
import com.mis.vo.FundOverviewVO;
import com.mis.vo.FundTrendVO;
import com.mis.vo.MonthlyAllocStatVO;
import com.mis.vo.OrgRankVO;
import com.mis.vo.SupervisionEffectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基金运行分析与监管成效分析
 * <p>数据源：经办结算单（mis_bill）、机构报盘（mis_settlement）、费用明细（mis_fee_item）、
 * 月度结算（mis_monthly）、日拨付（mis_daily_alloc）、参保人、协议机构，
 * 以及监管成效所需的审核 / 疑点 / 线索 / 事前事中 / 质控 / 追溯码 / 追回 / 自查自纠数据。
 */
@Service
@RequiredArgsConstructor
public class FundAnalysisServiceImpl implements FundAnalysisService {

    private final MisBillMapper billMapper;
    private final MisFeeItemMapper feeItemMapper;
    private final MisSettlementMapper settlementMapper;
    private final MisMonthlyMapper monthlyMapper;
    private final MisDailyAllocMapper dailyAllocMapper;
    private final MisInsuredPersonMapper personMapper;
    private final MisAgreementMapper agreementMapper;

    private final MisFeeAuditMapper auditMapper;
    private final MifSuspicionMapper suspicionMapper;
    private final MifFraudClueMapper clueMapper;
    private final MifPrecheckResultMapper precheckMapper;
    private final MifQualityResultMapper qualityMapper;
    private final MifTraceVerifyLogMapper traceLogMapper;
    private final MifRecoverMapper recoverMapper;
    private final MifSelfCheckMapper selfCheckMapper;

    // ------------------------------------------------------------------ 总览

    @Override
    public FundOverviewVO overview(int rangeDays) {
        int days = rangeDays <= 0 ? 30 : rangeDays;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        FundOverviewVO vo = new FundOverviewVO();
        vo.setRangeDays(days);
        vo.setStartDate(start.toString());
        vo.setEndDate(end.toString());

        // 参保人
        List<MisInsuredPerson> persons = personMapper.selectList(null);
        int active = 0;
        int suspended = 0;
        for (MisInsuredPerson p : persons) {
            if (p.getPersonStatus() != null && p.getPersonStatus() == 1) {
                active++;
            } else {
                suspended++;
            }
        }
        vo.setInsuredCount(persons.size());
        vo.setActiveInsuredCount(active);
        vo.setSuspendedInsuredCount(suspended);

        // 结算量（经办结算单 + 机构报盘）
        List<MisBill> bills = billsIn(start, end);
        List<MisSettlement> settlements = settlementsIn(start, end);
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal insPay = BigDecimal.ZERO;
        BigDecimal acct = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal self = BigDecimal.ZERO;
        for (MisBill b : bills) {
            cost = cost.add(nz(b.getTotalAmount()));
            insPay = insPay.add(nz(b.getInsurancePay()));
            acct = acct.add(nz(b.getPersonalAccount()));
            cash = cash.add(nz(b.getCashPay()));
            self = self.add(nz(b.getSelfPay()));
        }
        for (MisSettlement s : settlements) {
            cost = cost.add(nz(s.getTotalAmount()));
            insPay = insPay.add(nz(s.getInsurancePay()));
            self = self.add(nz(s.getSelfPay()));
        }
        int count = bills.size() + settlements.size();
        vo.setSettleCount(count);
        vo.setTotalCost(cost);
        vo.setInsurancePay(insPay);
        vo.setPersonalAccount(acct);
        vo.setCashPay(cash);
        vo.setSelfPay(self);

        // 基金统筹收入 = 统筹支付 + 个人账户支付；支出 = 月度已拨付合计
        BigDecimal income = insPay.add(acct);
        BigDecimal expense = BigDecimal.ZERO;
        for (MisMonthly m : monthlyMapper.selectList(null)) {
            expense = expense.add(nz(m.getAllocatedAmount()));
        }
        vo.setFundIncome(income);
        vo.setFundExpense(expense);
        vo.setFundBalance(income.subtract(expense));
        vo.setFundRate(rate(expense, income));
        vo.setPayRatio(rate(insPay, cost));
        vo.setAvgCost(divide(cost, count));
        vo.setPerCapitaCost(divide(cost, persons.size()));
        vo.setPerCapitaExpense(divide(expense, persons.size()));
        return vo;
    }

    // ------------------------------------------------------------------ 维度聚合

    @Override
    public List<DimensionStatVO> byInsuranceType(int rangeDays) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays((rangeDays <= 0 ? 30 : rangeDays) - 1L);
        Map<String, DimensionStatVO> map = new LinkedHashMap<>();
        for (MisSettlement s : settlementsIn(start, end)) {
            accumulate(map, nz(s.getInsuranceType()), nz(s.getTotalAmount()), nz(s.getInsurancePay()), 1);
        }
        for (MisBill b : billsIn(start, end)) {
            accumulate(map, nz(b.getInsuranceType()), nz(b.getTotalAmount()), nz(b.getInsurancePay()), 1);
        }
        return finish(map);
    }

    @Override
    public List<DimensionStatVO> byEncounterType(int rangeDays) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays((rangeDays <= 0 ? 30 : rangeDays) - 1L);
        Map<String, DimensionStatVO> map = new LinkedHashMap<>();
        for (MisSettlement s : settlementsIn(start, end)) {
            accumulate(map, nz(s.getEncounterType()), nz(s.getTotalAmount()), nz(s.getInsurancePay()), 1);
        }
        for (MisBill b : billsIn(start, end)) {
            accumulate(map, nz(b.getSettleType()), nz(b.getTotalAmount()), nz(b.getInsurancePay()), 1);
        }
        return finish(map);
    }

    @Override
    public List<DimensionStatVO> byPayClass(int rangeDays) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays((rangeDays <= 0 ? 30 : rangeDays) - 1L);
        Map<String, DimensionStatVO> map = new LinkedHashMap<>();
        for (MisFeeItem it : itemsIn(start, end)) {
            accumulate(map, nz(it.getPayClass()), nz(it.getAmount()), BigDecimal.ZERO, 0);
        }
        return finish(map);
    }

    @Override
    public List<DimensionStatVO> byCategory(int rangeDays) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays((rangeDays <= 0 ? 30 : rangeDays) - 1L);
        Map<String, DimensionStatVO> map = new LinkedHashMap<>();
        for (MisFeeItem it : itemsIn(start, end)) {
            accumulate(map, nz(it.getCategory()), nz(it.getAmount()), BigDecimal.ZERO, 0);
        }
        return finish(map);
    }

    // ------------------------------------------------------------------ 机构排名

    @Override
    public List<OrgRankVO> orgRank(int rangeDays, int limit) {
        int days = rangeDays <= 0 ? 30 : rangeDays;
        int top = limit <= 0 ? 10 : limit;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);

        Map<String, OrgRankVO> map = new LinkedHashMap<>();
        Map<String, String> nameByCode = new HashMap<>();
        for (MisAgreement a : agreementMapper.selectList(null)) {
            nameByCode.put(nz(a.getMedinsCode()), a.getMedinsName());
        }
        for (MisSettlement s : settlementsIn(start, end)) {
            String code = nz(s.getFixMedinsCode());
            OrgRankVO o = map.computeIfAbsent(code, k -> {
                OrgRankVO v = new OrgRankVO();
                v.setMedinsCode(k);
                v.setMedinsName(nameByCode.getOrDefault(k, "未签约机构(" + k + ")"));
                v.setDeductAmount(BigDecimal.ZERO);
                v.setDeductCount(0);
                return v;
            });
            o.setSettleCount(o.getSettleCount() + 1);
            o.setTotalAmount(o.getTotalAmount().add(nz(s.getTotalAmount())));
            o.setInsurancePay(o.getInsurancePay().add(nz(s.getInsurancePay())));
        }
        // 违规扣减（追回单）并入机构维度
        for (MifRecover r : recoverMapper.selectList(null)) {
            String code = nz(r.getTargetOrgCode());
            if (code.isBlank()) {
                continue;
            }
            OrgRankVO o = map.computeIfAbsent(code, k -> {
                OrgRankVO v = new OrgRankVO();
                v.setMedinsCode(k);
                v.setMedinsName(nameByCode.getOrDefault(k, r.getTargetOrgName() != null ? r.getTargetOrgName() : k));
                v.setDeductAmount(BigDecimal.ZERO);
                v.setDeductCount(0);
                return v;
            });
            o.setDeductAmount(o.getDeductAmount().add(nz(r.getRecoverAmount())));
            o.setDeductCount(o.getDeductCount() + 1);
        }
        List<OrgRankVO> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparing(OrgRankVO::getTotalAmount, Comparator.nullsLast(Comparator.reverseOrder())));
        for (int i = 0; i < list.size(); i++) {
            OrgRankVO o = list.get(i);
            o.setRank(i + 1);
            o.setAvgAmount(divide(o.getTotalAmount(), o.getSettleCount()));
        }
        return list.stream().limit(top).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ 趋势

    @Override
    public List<FundTrendVO> trend(int rangeDays) {
        int days = rangeDays <= 0 ? 30 : rangeDays;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        Map<String, FundTrendVO> map = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            FundTrendVO p = new FundTrendVO();
            p.setDay(d.toString());
            p.setCount(0);
            p.setAmount(BigDecimal.ZERO);
            p.setInsurancePay(BigDecimal.ZERO);
            p.setPersonalAccount(BigDecimal.ZERO);
            p.setCashPay(BigDecimal.ZERO);
            p.setAllocAmount(BigDecimal.ZERO);
            map.put(d.toString(), p);
        }
        for (MisSettlement s : settlementMapper.selectList(new LambdaQueryWrapper<MisSettlement>()
                .eq(MisSettlement::getStatus, 1)
                .between(MisSettlement::getBillDate, start, end))) {
            FundTrendVO p = map.get(String.valueOf(s.getBillDate()));
            if (p == null) {
                continue;
            }
            p.setCount(p.getCount() + 1);
            p.setAmount(p.getAmount().add(nz(s.getTotalAmount())));
            p.setInsurancePay(p.getInsurancePay().add(nz(s.getInsurancePay())));
        }
        for (MisBill b : billsIn(start, end)) {
            if (b.getSettleTime() == null) {
                continue;
            }
            FundTrendVO p = map.get(b.getSettleTime().toLocalDate().toString());
            if (p == null) {
                continue;
            }
            p.setCount(p.getCount() + 1);
            p.setAmount(p.getAmount().add(nz(b.getTotalAmount())));
            p.setInsurancePay(p.getInsurancePay().add(nz(b.getInsurancePay())));
            p.setPersonalAccount(p.getPersonalAccount().add(nz(b.getPersonalAccount())));
            p.setCashPay(p.getCashPay().add(nz(b.getCashPay())));
        }
        for (MisDailyAlloc a : dailyAllocMapper.selectList(null)) {
            FundTrendVO p = map.get(a.getBizDate());
            if (p == null) {
                continue;
            }
            p.setAllocAmount(p.getAllocAmount().add(nz(a.getPayAmount())));
        }
        return new ArrayList<>(map.values());
    }

    // ------------------------------------------------------------------ 月度拨付

    @Override
    public List<MonthlyAllocStatVO> monthlyAlloc() {
        List<MisMonthly> months = monthlyMapper.selectList(new LambdaQueryWrapper<MisMonthly>()
                .orderByDesc(MisMonthly::getMonth));
        List<MonthlyAllocStatVO> list = new ArrayList<>();
        for (MisMonthly m : months) {
            MonthlyAllocStatVO v = new MonthlyAllocStatVO();
            v.setMonth(m.getMonth());
            v.setDeclareCount(m.getDeclareCount());
            v.setDeclareAmount(m.getDeclareAmount());
            v.setDeductAmount(m.getDeductAmount());
            v.setAllocatedAmount(m.getAllocatedAmount());
            v.setDeductRate(rate(m.getDeductAmount(), m.getDeclareAmount()));
            v.setStatus(m.getStatus());
            list.add(v);
        }
        return list;
    }

    // ------------------------------------------------------------------ 监管成效

    @Override
    public SupervisionEffectVO supervisionEffect() {
        SupervisionEffectVO vo = new SupervisionEffectVO();

        // 智能审核
        List<MisFeeAudit> audits = auditMapper.selectList(null);
        int passed = 0;
        int rejected = 0;
        int appealing = 0;
        int conferred = 0;
        BigDecimal auditAmount = BigDecimal.ZERO;
        for (MisFeeAudit a : audits) {
            String s = nz(a.getStatus());
            if ("已通过".equals(s) || "复审通过".equals(s)) {
                passed++;
            } else if ("已驳回".equals(s) || "复审维持".equals(s)) {
                rejected++;
            } else if ("已申诉".equals(s)) {
                appealing++;
            } else if ("已合议".equals(s)) {
                conferred++;
            }
            auditAmount = auditAmount.add(nz(a.getAmount()));
        }
        vo.setAuditTotal(audits.size());
        vo.setAuditPassed(passed);
        vo.setAuditRejected(rejected);
        vo.setAuditAppealing(appealing);
        vo.setAuditConferred(conferred);
        vo.setAuditPassRate(rate(BigDecimal.valueOf(passed), BigDecimal.valueOf(audits.size())));
        vo.setAuditAmount(auditAmount);

        // 疑点稽核
        List<MifSuspicion> suspicions = suspicionMapper.selectList(null);
        int sPending = 0;
        int sConfirmed = 0;
        int sExcluded = 0;
        BigDecimal sAmount = BigDecimal.ZERO;
        for (MifSuspicion s : suspicions) {
            String st = nz(s.getStatus());
            if ("待处理".equals(st)) {
                sPending++;
            } else if ("已确认".equals(st)) {
                sConfirmed++;
            } else if ("已排除".equals(st)) {
                sExcluded++;
            }
            sAmount = sAmount.add(nz(s.getInvolvedAmount()));
        }
        vo.setSuspicionTotal(suspicions.size());
        vo.setSuspicionPending(sPending);
        vo.setSuspicionConfirmed(sConfirmed);
        vo.setSuspicionExcluded(sExcluded);
        vo.setSuspicionAmount(sAmount);

        // 反欺诈线索
        List<MifFraudClue> clues = clueMapper.selectList(null);
        int cPending = 0;
        int cConfirmed = 0;
        int cExcluded = 0;
        int cTransferred = 0;
        for (MifFraudClue c : clues) {
            if (c.getStatus() == null) {
                cPending++;
            } else if (c.getStatus() == 1) {
                cPending++;
            } else if (c.getStatus() == 2) {
                cConfirmed++;
            } else if (c.getStatus() == 3) {
                cExcluded++;
            } else if (c.getStatus() == 4) {
                cTransferred++;
            }
        }
        vo.setClueTotal(clues.size());
        vo.setCluePending(cPending);
        vo.setClueConfirmed(cConfirmed);
        vo.setClueExcluded(cExcluded);
        vo.setClueTransferred(cTransferred);

        // 事前提醒/事中拦截
        List<MifPrecheckResult> prechecks = precheckMapper.selectList(null);
        int blocked = 0;
        int pPending = 0;
        for (MifPrecheckResult p : prechecks) {
            if (p.getBlockFlag() != null && p.getBlockFlag() == 1) {
                blocked++;
            }
            if (p.getHandleStatus() != null && p.getHandleStatus() == 0) {
                pPending++;
            }
        }
        vo.setPrecheckTotal(prechecks.size());
        vo.setPrecheckBlocked(blocked);
        vo.setPrecheckPending(pPending);

        // 结算清单质控
        List<MifQualityResult> qualities = qualityMapper.selectList(null);
        int qSerious = 0;
        int qPending = 0;
        BigDecimal qDeduct = BigDecimal.ZERO;
        for (MifQualityResult q : qualities) {
            if (q.getSeverity() != null && q.getSeverity() == 3) {
                qSerious++;
            }
            if (q.getHandleStatus() != null && q.getHandleStatus() == 0) {
                qPending++;
            }
            qDeduct = qDeduct.add(nz(q.getDeductAmount()));
        }
        vo.setQualityTotal(qualities.size());
        vo.setQualitySerious(qSerious);
        vo.setQualityPending(qPending);
        vo.setQualityDeduct(qDeduct);

        // 追溯码
        List<MifTraceVerifyLog> traces = traceLogMapper.selectList(null);
        int tPassed = 0;
        for (MifTraceVerifyLog t : traces) {
            if (t.getVerifyResult() != null && t.getVerifyResult() == 1) {
                tPassed++;
            }
        }
        vo.setTraceTotal(traces.size());
        vo.setTracePassed(tPassed);
        vo.setTraceAbnormal(traces.size() - tPassed);

        // 违规追回
        List<MifRecover> recovers = recoverMapper.selectList(null);
        int rPending = 0;
        BigDecimal rAmount = BigDecimal.ZERO;
        for (MifRecover r : recovers) {
            if ("待追回".equals(r.getStatus())) {
                rPending++;
            }
            rAmount = rAmount.add(nz(r.getRecoverAmount()));
        }
        vo.setRecoverTotal(recovers.size());
        vo.setRecoverAmount(rAmount);
        vo.setRecoverPending(rPending);

        // 自查自纠
        List<MifSelfCheck> selfChecks = selfCheckMapper.selectList(null);
        int done = 0;
        for (MifSelfCheck s : selfChecks) {
            if (s.getStatus() != null && s.getStatus() == 5) {
                done++;
            }
        }
        vo.setSelfCheckTotal(selfChecks.size());
        vo.setSelfCheckDone(done);
        vo.setSelfCheckDoneRate(rate(BigDecimal.valueOf(done), BigDecimal.valueOf(selfChecks.size())));
        return vo;
    }

    // ------------------------------------------------------------------ 内部

    private List<MisBill> billsIn(LocalDate start, LocalDate end) {
        return billMapper.selectList(new LambdaQueryWrapper<MisBill>()
                .ne(MisBill::getStatus, "已退费")
                .ge(MisBill::getSettleTime, start.atStartOfDay())
                .lt(MisBill::getSettleTime, end.plusDays(1).atStartOfDay()));
    }

    private List<MisSettlement> settlementsIn(LocalDate start, LocalDate end) {
        return settlementMapper.selectList(new LambdaQueryWrapper<MisSettlement>()
                .eq(MisSettlement::getStatus, 1)
                .between(MisSettlement::getBillDate, start, end));
    }

    /** 费用明细（限定在区间内结算单的明细上） */
    private List<MisFeeItem> itemsIn(LocalDate start, LocalDate end) {
        List<Long> billIds = new ArrayList<>();
        for (MisBill b : billsIn(start, end)) {
            billIds.add(b.getId());
        }
        if (billIds.isEmpty()) {
            return List.of();
        }
        return feeItemMapper.selectList(new LambdaQueryWrapper<MisFeeItem>()
                .eq(MisFeeItem::getOwnerType, 2)
                .in(MisFeeItem::getOwnerId, billIds));
    }

    private void accumulate(Map<String, DimensionStatVO> map, String name, BigDecimal amount,
                            BigDecimal insPay, int countInc) {
        DimensionStatVO v = map.computeIfAbsent(name, k -> {
            DimensionStatVO d = new DimensionStatVO();
            d.setName(k);
            d.setCount(0);
            d.setAmount(BigDecimal.ZERO);
            d.setInsurancePay(BigDecimal.ZERO);
            return d;
        });
        if (countInc > 0) {
            v.setCount(v.getCount() + countInc);
        }
        v.setAmount(v.getAmount().add(amount));
        v.setInsurancePay(v.getInsurancePay().add(insPay));
    }

    /** 计算占比并按金额倒序 */
    private List<DimensionStatVO> finish(Map<String, DimensionStatVO> map) {
        BigDecimal total = BigDecimal.ZERO;
        for (DimensionStatVO v : map.values()) {
            total = total.add(v.getAmount());
        }
        List<DimensionStatVO> list = new ArrayList<>(map.values());
        for (DimensionStatVO v : list) {
            v.setRate(rate(v.getAmount(), total));
            v.setAvgAmount(divide(v.getAmount(), v.getCount()));
        }
        list.sort(Comparator.comparing(DimensionStatVO::getAmount, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private BigDecimal rate(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal divide(BigDecimal amount, int count) {
        if (amount == null || count <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
