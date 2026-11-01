package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.entity.MisBill;
import com.mis.entity.MisFeeAudit;
import com.mis.entity.MisPendingSettle;
import com.mis.entity.MisRecon;
import com.mis.entity.MifRecover;
import com.mis.entity.MifSuspicion;
import com.mis.mapper.MisBillMapper;
import com.mis.mapper.MisFeeAuditMapper;
import com.mis.mapper.MisPendingSettleMapper;
import com.mis.mapper.MisReconMapper;
import com.mis.mapper.MifRecoverMapper;
import com.mis.mapper.MifSuspicionMapper;
import com.mis.service.WorkbenchService;
import com.mis.vo.BillVO;
import com.mis.vo.StatsVO;
import com.mis.vo.WorkbenchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkbenchServiceImpl implements WorkbenchService {

    private final MisBillMapper billMapper;
    private final MisPendingSettleMapper pendingMapper;
    private final MisFeeAuditMapper auditMapper;
    private final MisReconMapper reconMapper;
    private final MifSuspicionMapper suspicionMapper;
    private final MifRecoverMapper recoverMapper;

    @Override
    public WorkbenchVO summary() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<MisBill> todays = billMapper.selectList(new LambdaQueryWrapper<MisBill>()
                .ge(MisBill::getSettleTime, start)
                .lt(MisBill::getSettleTime, end));

        WorkbenchVO.Kpi kpi = new WorkbenchVO.Kpi();
        kpi.setTodayCount(todays.size());
        kpi.setTodayAmount(sum(todays, MisBill::getTotalAmount));
        kpi.setInsurancePay(sum(todays, MisBill::getInsurancePay));
        kpi.setPersonalAccount(sum(todays, MisBill::getPersonalAccount));
        kpi.setCashPay(sum(todays, MisBill::getCashPay));

        long pending = pendingMapper.selectCount(new LambdaQueryWrapper<MisPendingSettle>()
                .eq(MisPendingSettle::getStatus, 1));
        long audit = auditMapper.selectCount(new LambdaQueryWrapper<MisFeeAudit>()
                .eq(MisFeeAudit::getStatus, "待审核"));
        long reconcile = reconMapper.selectCount(new LambdaQueryWrapper<MisRecon>()
                .eq(MisRecon::getStatus, "待对账"));
        long refund = billMapper.selectCount(new LambdaQueryWrapper<MisBill>()
                .eq(MisBill::getStatus, "已退费"));
        long suspicion = suspicionMapper.selectCount(new LambdaQueryWrapper<MifSuspicion>()
                .eq(MifSuspicion::getStatus, "待核实"));
        long recover = recoverMapper.selectCount(new LambdaQueryWrapper<MifRecover>()
                .eq(MifRecover::getStatus, "待追回"));

        WorkbenchVO.Todo todo = new WorkbenchVO.Todo();
        todo.setPendingSettle((int) pending);
        todo.setAudit((int) audit);
        todo.setReconcile((int) reconcile);
        todo.setRefund((int) refund);
        todo.setSuspicion((int) suspicion);
        todo.setRecover((int) recover);
        todo.setTotal((int) (pending + audit + reconcile + suspicion + recover));

        List<MisBill> recent = billMapper.selectPage(new Page<>(1, 6),
                new LambdaQueryWrapper<MisBill>().orderByDesc(MisBill::getSettleTime)).getRecords();
        List<BillVO> recentVO = new ArrayList<>();
        for (MisBill b : recent) {
            BillVO vo = new BillVO();
            BeanUtils.copyProperties(b, vo);
            recentVO.add(vo);
        }

        WorkbenchVO vo = new WorkbenchVO();
        vo.setKpi(kpi);
        vo.setTodo(todo);
        vo.setRecentBills(recentVO);
        return vo;
    }

    @Override
    public StatsVO stats() {
        List<MisBill> all = billMapper.selectList(null);

        Map<String, StatsVO.InsStat> byIns = new LinkedHashMap<>();
        Map<String, StatsVO.DeptStat> byDept = new LinkedHashMap<>();
        for (MisBill b : all) {
            StatsVO.InsStat ins = byIns.computeIfAbsent(b.getInsuranceType(), k -> {
                StatsVO.InsStat s = new StatsVO.InsStat();
                s.setAmount(z());
                s.setInsurancePay(z());
                return s;
            });
            ins.setCount(ins.getCount() + 1);
            ins.setAmount(nz(ins.getAmount()).add(nz(b.getTotalAmount())));
            ins.setInsurancePay(nz(ins.getInsurancePay()).add(nz(b.getInsurancePay())));

            StatsVO.DeptStat dept = byDept.computeIfAbsent(b.getDept(), k -> {
                StatsVO.DeptStat s = new StatsVO.DeptStat();
                s.setAmount(z());
                return s;
            });
            dept.setCount(dept.getCount() + 1);
            dept.setAmount(nz(dept.getAmount()).add(nz(b.getTotalAmount())));
        }

        LocalDate today = LocalDate.now();
        List<StatsVO.TrendPoint> trend = new ArrayList<>();
        for (int i = 9; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime s = d.atStartOfDay();
            LocalDateTime e = d.plusDays(1).atStartOfDay();
            List<MisBill> dayBills = billMapper.selectList(new LambdaQueryWrapper<MisBill>()
                    .ge(MisBill::getSettleTime, s).lt(MisBill::getSettleTime, e));
            StatsVO.TrendPoint p = new StatsVO.TrendPoint();
            p.setDay(d.toString());
            p.setCount(dayBills.size());
            p.setAmount(sum(dayBills, MisBill::getTotalAmount));
            trend.add(p);
        }

        StatsVO vo = new StatsVO();
        vo.setByIns(byIns);
        vo.setByDept(byDept);
        vo.setTrend(trend);
        return vo;
    }

    private BigDecimal sum(List<MisBill> list, java.util.function.Function<MisBill, BigDecimal> getter) {
        BigDecimal total = z();
        for (MisBill b : list) {
            total = total.add(nz(getter.apply(b)));
        }
        return total;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? z() : v;
    }

    private static BigDecimal z() {
        return BigDecimal.ZERO.setScale(2);
    }
}
