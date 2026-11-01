package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.DailyQueryDTO;
import com.mis.entity.MisSettlement;
import com.mis.entity.MisDailyAlloc;
import com.mis.mapper.MisSettlementMapper;
import com.mis.mapper.MisDailyAllocMapper;
import com.mis.service.DailyAllocService;
import com.mis.vo.ActionVO;
import com.mis.vo.DailyAllocVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyAllocServiceImpl implements DailyAllocService {

    /** 日拨付比例（即时结算改革：原则上按笔拨付比例 60%） */
    private static final BigDecimal DAILY_RATIO = new BigDecimal("0.6");

    private final MisDailyAllocMapper dailyMapper;
    private final MisSettlementMapper settlementMapper;

    @Override
    public ActionVO generate(String bizDate) {
        if (bizDate == null || bizDate.trim().isEmpty()) {
            return ActionVO.fail("请选择业务日期");
        }
        Long exist = dailyMapper.selectCount(new LambdaQueryWrapper<MisDailyAlloc>()
                .eq(MisDailyAlloc::getBizDate, bizDate));
        if (exist != null && exist > 0) {
            return ActionVO.fail(bizDate + " 已生成日拨付批次，不可重复生成");
        }
        List<MisSettlement> settled = settlementMapper.selectList(new LambdaQueryWrapper<MisSettlement>()
                .eq(MisSettlement::getStatus, 1)
                .eq(MisSettlement::getBillDate, LocalDate.parse(bizDate)));
        if (settled.isEmpty()) {
            return ActionVO.fail(bizDate + " 无已接收的有效结算单，无法生成日拨付");
        }
        BigDecimal totalFund = settled.stream()
                .map(s -> s.getInsurancePay() == null ? BigDecimal.ZERO : s.getInsurancePay())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payAmount = totalFund.multiply(DAILY_RATIO).setScale(2, RoundingMode.HALF_UP);

        MisDailyAlloc alloc = new MisDailyAlloc();
        alloc.setBizDate(bizDate);
        alloc.setBatchNo("YBD" + bizDate.replace("-", ""));
        alloc.setSettleCount(settled.size());
        alloc.setTotalFund(totalFund.setScale(2, RoundingMode.HALF_UP));
        alloc.setPayAmount(payAmount);
        alloc.setRemitStatus(0);
        alloc.setRemark("即时结算按笔日拨付，比例 " + DAILY_RATIO.toPlainString());
        dailyMapper.insert(alloc);
        return ActionVO.ok();
    }

    @Override
    public PageVO<DailyAllocVO> page(DailyQueryDTO q) {
        Page<MisDailyAlloc> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisDailyAlloc> w = new LambdaQueryWrapper<>();
        if (hasText(q.getBizDate())) {
            w.eq(MisDailyAlloc::getBizDate, q.getBizDate());
        }
        if (q.getRemitStatus() != null) {
            w.eq(MisDailyAlloc::getRemitStatus, q.getRemitStatus());
        }
        w.orderByDesc(MisDailyAlloc::getBizDate);
        Page<MisDailyAlloc> res = dailyMapper.selectPage(page, w);
        List<DailyAllocVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO remit(Long id) {
        MisDailyAlloc a = dailyMapper.selectById(id);
        if (a == null) {
            return ActionVO.fail("日拨付批次不存在");
        }
        if (a.getRemitStatus() != null && a.getRemitStatus() == 1) {
            return ActionVO.fail("该批次已回单");
        }
        a.setRemitStatus(1);
        a.setReceiptNo("ER" + a.getBatchNo());
        dailyMapper.updateById(a);
        return ActionVO.ok();
    }

    private DailyAllocVO toVO(MisDailyAlloc a) {
        DailyAllocVO vo = new DailyAllocVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
