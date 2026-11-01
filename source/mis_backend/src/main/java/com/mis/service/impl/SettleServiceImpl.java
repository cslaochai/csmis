package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.BillQueryDTO;
import com.mis.dto.BizPageParam;
import com.mis.engine.SettleEngine;
import com.mis.entity.MisBill;
import com.mis.entity.MisFeeAudit;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MisPendingSettle;
import com.mis.mapper.MisBillMapper;
import com.mis.mapper.MisFeeAuditMapper;
import com.mis.mapper.MisFeeItemMapper;
import com.mis.mapper.MisPendingSettleMapper;
import com.mis.service.PrecheckService;
import com.mis.service.SettleService;
import com.mis.service.StockService;
import com.mis.vo.BillVO;
import com.mis.vo.DecVO;
import com.mis.vo.FeeItemVO;
import com.mis.vo.PendingSettleVO;
import com.mis.vo.PrecheckVO;
import com.mis.vo.SettleResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import com.mis.service.impl.TodoServiceImpl;
import com.mis.service.SettleListService;

@Service
@RequiredArgsConstructor
public class SettleServiceImpl implements SettleService {

    private final MisPendingSettleMapper pendingMapper;
    private final MisBillMapper billMapper;
    private final MisFeeItemMapper feeItemMapper;
    private final MisFeeAuditMapper auditMapper;
    private final SettleEngine settleEngine;
    private final PrecheckService precheckService;
    private final StockService stockService;
    private final SettleListService settleListService;

    @Override
    public PageVO<PendingSettleVO> pendingPage(int bizType, BizPageParam query) {
        Page<MisPendingSettle> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<MisPendingSettle> w = new LambdaQueryWrapper<>();
        w.eq(MisPendingSettle::getBizType, bizType)
                .eq(MisPendingSettle::getStatus, 1);
        if (hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(q -> q.like(MisPendingSettle::getPatientName, kw)
                    .or().like(MisPendingSettle::getBizNo, kw));
        }
        w.orderByAsc(MisPendingSettle::getId);
        Page<MisPendingSettle> res = pendingMapper.selectPage(page, w);
        List<PendingSettleVO> list = res.getRecords().stream().map(this::toPendingVO).toList();
        return PageVO.of(list, res.getTotal());
    }

    @Override
    public DecVO preview(String bizNo) {
        MisPendingSettle p = loadPending(bizNo);
        List<MisFeeItem> items = itemsOf(1, p.getId());
        DecVO dec = settleEngine.compute(items, p.getInsuranceType());
        // 第一道防线：结算前实时比对，命中提醒随试算结果一并返回
        PrecheckVO pre = precheckService.check(p, items, dec);
        dec.setPrecheckList(pre.getHits());
        dec.setPrecheckBlockFlag(pre.getBlockFlag());
        return dec;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettleResultVO confirm(int bizType, String bizNo) {
        MisPendingSettle p = pendingMapper.selectOne(new LambdaQueryWrapper<MisPendingSettle>()
                .eq(MisPendingSettle::getBizNo, bizNo)
                .eq(MisPendingSettle::getBizType, bizType)
                .eq(MisPendingSettle::getStatus, 1)
                .last("limit 1"));
        if (p == null) {
            return SettleResultVO.fail("业务已结算或不存在");
        }
        List<MisFeeItem> srcItems = itemsOf(1, p.getId());
        DecVO d = settleEngine.compute(srcItems, p.getInsuranceType());

        // 第二道防线：结算关口实时拦截，命中阻断规则直接拒付，高风险转人工复核
        PrecheckVO inter = precheckService.intercept(p, srcItems, d);
        if (inter.getBlockFlag() != null && inter.getBlockFlag() == 1) {
            return SettleResultVO.fail("事中拦截：" + firstReason(inter));
        }
        boolean toManual = inter.getHitCount() != null && inter.getHitCount() > 0
                && inter.getMaxRiskLevel() != null && inter.getMaxRiskLevel() >= 3;

        String settleType = bizType == 2 ? "住院" : "门诊";
        MisBill bill = new MisBill();
        bill.setPendingId(p.getId());
        bill.setBizNo(p.getBizNo());
        bill.setAdmitDays(p.getAdmitDays());
        bill.setPatientName(p.getPatientName());
        bill.setIdCard(p.getIdCard());
        bill.setIdCardMask(p.getIdCardMask());
        bill.setInsuranceType(p.getInsuranceType());
        bill.setSettleType(settleType);
        bill.setDept(p.getDept());
        bill.setDoctor(p.getDoctor());
        bill.setSettleTime(LocalDateTime.now());
        bill.setStatus(toManual ? "待审核" : "已结算");
        bill.setTotalAmount(d.getTotal());
        bill.setInsurancePay(d.getInsurancePay());
        bill.setPersonalAccount(d.getPersonalAccount());
        bill.setCashPay(d.getCashPay());
        bill.setSelfPay(d.getSelfPay());
        bill.setDeductible(d.getDeductible());
        bill.setBasePool(d.getBasePool());
        billMapper.insert(bill);

        String prefix = bizType == 2 ? "ZY" : "YL";
        bill.setBillNo(prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", bill.getId()));
        billMapper.updateById(bill);

        for (MisFeeItem src : srcItems) {
            MisFeeItem dst = new MisFeeItem();
            BeanUtils.copyProperties(src, dst);
            dst.setId(null);
            dst.setOwnerType(2);
            dst.setOwnerId(bill.getId());
            dst.setReversed(false);
            dst.setCreateTime(null);
            feeItemMapper.insert(dst);
        }

        // 转人工：生成费用审核待办，交由人工复核后扣款
        if (toManual) {
            MisFeeAudit task = new MisFeeAudit();
            task.setBillId(bill.getId());
            task.setBillNo(bill.getBillNo());
            task.setPatientName(bill.getPatientName());
            task.setItemName(settleType + "结算单整体");
            task.setCategory(settleType);
            task.setPayClass("甲");
            task.setAmount(d.getTotal());
            task.setReason("事中审核转人工：" + firstReason(inter));
            task.setStatus("待审核");
            auditMapper.insert(task);

            // 待办中心推送：审核员收到「结算单待复核」待办
            TodoServiceImpl.pushTodo(
                    "mis_fee_audit",
                    "费用审核待办：" + settleType + "结算单 " + bill.getBillNo() + " 命中监管规则需人工复核",
                    "mis_fee_audit", task.getId(), bill.getBillNo(),
                    "审核员B", 3L, 1);
        }

        // 进销存联动：结算成功后按药品/耗材类明细出库
        stockService.issueForBill(bill, itemsOf(2, bill.getId()));

        // 结算清单主表：生成报送用清单（幂等）
        settleListService.generate(bill, itemsOf(2, bill.getId()));

        p.setStatus(2);
        pendingMapper.updateById(p);

        return SettleResultVO.ok(toBillVO(bill, true));
    }

    @Override
    public PageVO<BillVO> billPage(BillQueryDTO q) {
        Page<MisBill> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisBill> w = new LambdaQueryWrapper<>();
        if (hasText(q.getBillNo())) {
            w.like(MisBill::getBillNo, q.getBillNo().trim());
        }
        if (hasText(q.getPatientName())) {
            w.like(MisBill::getPatientName, q.getPatientName().trim());
        }
        if (hasText(q.getInsuranceType())) {
            w.eq(MisBill::getInsuranceType, q.getInsuranceType());
        }
        if (hasText(q.getSettleType())) {
            w.eq(MisBill::getSettleType, q.getSettleType());
        }
        if (hasText(q.getStatus())) {
            w.eq(MisBill::getStatus, q.getStatus());
        }
        w.orderByDesc(MisBill::getSettleTime);
        Page<MisBill> res = billMapper.selectPage(page, w);
        List<BillVO> list = res.getRecords().stream().map(b -> toBillVO(b, false)).toList();
        return PageVO.of(list, res.getTotal());
    }

    @Override
    public BillVO billDetail(Long id) {
        MisBill bill = billMapper.selectById(id);
        if (bill == null) {
            return null;
        }
        return toBillVO(bill, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettleResultVO reverse(Long id) {
        MisBill bill = billMapper.selectById(id);
        if (bill == null) {
            return SettleResultVO.fail("结算单不存在");
        }
        if ("已退费".equals(bill.getStatus()) || "异常".equals(bill.getStatus())) {
            return SettleResultVO.fail("当前状态不可退费");
        }
        bill.setStatus("已退费");
        billMapper.updateById(bill);
        List<MisFeeItem> items = itemsOf(2, bill.getId());
        for (MisFeeItem it : items) {
            it.setReversed(true);
            feeItemMapper.updateById(it);
        }
        // 进销存联动：退费红冲回补库存
        stockService.rollbackForBill(bill.getBillNo());
        // 结算清单联动：退费后清单作废
        settleListService.voidList(bill.getId(), "退费冲正，清单作废");
        return SettleResultVO.ok(null);
    }

    // ------------------------------------------------------------------

    /** 取第一条命中原因作为拦截/转人工的说明 */
    private String firstReason(PrecheckVO pre) {
        if (pre == null || pre.getHits() == null || pre.getHits().isEmpty()) {
            return "命中事中审核规则";
        }
        String s = pre.getHits().get(0).getPromptText();
        return s == null || s.isBlank() ? "命中事中审核规则" : s;
    }

    private MisPendingSettle loadPending(String bizNo) {
        return pendingMapper.selectOne(new LambdaQueryWrapper<MisPendingSettle>()
                .eq(MisPendingSettle::getBizNo, bizNo)
                .eq(MisPendingSettle::getStatus, 1)
                .last("limit 1"));
    }

    private List<MisFeeItem> itemsOf(int ownerType, Long ownerId) {
        return feeItemMapper.selectList(new LambdaQueryWrapper<MisFeeItem>()
                .eq(MisFeeItem::getOwnerType, ownerType)
                .eq(MisFeeItem::getOwnerId, ownerId)
                .orderByAsc(MisFeeItem::getId));
    }

    private PendingSettleVO toPendingVO(MisPendingSettle p) {
        PendingSettleVO vo = new PendingSettleVO();
        BeanUtils.copyProperties(p, vo);
        vo.setItems(itemsOf(1, p.getId()).stream().map(this::toFeeItemVO).collect(Collectors.toList()));
        return vo;
    }

    private BillVO toBillVO(MisBill b, boolean withItems) {
        BillVO vo = new BillVO();
        BeanUtils.copyProperties(b, vo);
        if (withItems) {
            vo.setItems(itemsOf(2, b.getId()).stream().map(this::toFeeItemVO).collect(Collectors.toList()));
        }
        return vo;
    }

    private FeeItemVO toFeeItemVO(MisFeeItem it) {
        FeeItemVO vo = new FeeItemVO();
        BeanUtils.copyProperties(it, vo);
        return vo;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
