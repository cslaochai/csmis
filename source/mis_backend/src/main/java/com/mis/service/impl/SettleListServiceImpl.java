package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.SettleListQueryDTO;
import com.mis.entity.MisBill;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MisSettleList;
import com.mis.mapper.MisSettleListMapper;
import com.mis.service.SettleListService;
import com.mis.vo.ActionVO;
import com.mis.vo.SettleListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 结算清单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettleListServiceImpl implements SettleListService {

    private final MisSettleListMapper listMapper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Override
    public PageVO<SettleListVO> page(SettleListQueryDTO q) {
        LambdaQueryWrapper<MisSettleList> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getListNo())) {
            w.like(MisSettleList::getListNo, q.getListNo().trim());
        }
        if (StringUtils.hasText(q.getBillNo())) {
            w.like(MisSettleList::getBillNo, q.getBillNo().trim());
        }
        if (StringUtils.hasText(q.getMedinsCode())) {
            w.eq(MisSettleList::getMedinsCode, q.getMedinsCode());
        }
        if (StringUtils.hasText(q.getPatientName())) {
            w.like(MisSettleList::getPatientName, q.getPatientName());
        }
        if (StringUtils.hasText(q.getInsuranceType())) {
            w.eq(MisSettleList::getInsuranceType, q.getInsuranceType());
        }
        if (StringUtils.hasText(q.getEncounterType())) {
            w.eq(MisSettleList::getEncounterType, q.getEncounterType());
        }
        if (q.getCheckStatus() != null) {
            w.eq(MisSettleList::getCheckStatus, q.getCheckStatus());
        }
        if (StringUtils.hasText(q.getUploadBatchNo())) {
            w.eq(MisSettleList::getUploadBatchNo, q.getUploadBatchNo());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MisSettleList::getListNo, k)
                    .or().like(MisSettleList::getBillNo, k)
                    .or().like(MisSettleList::getPatientName, k));
        }
        w.orderByDesc(MisSettleList::getId);

        IPage<MisSettleList> pg = listMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        List<SettleListVO> list = pg.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(list, pg.getTotal());
    }

    @Override
    public ActionVO generate(MisBill bill, List<MisFeeItem> items) {
        if (bill == null || bill.getId() == null) {
            return ActionVO.fail("结算单不存在");
        }
        // 幂等：同一结算单已有未作废清单则跳过
        Long exists = listMapper.selectCount(new LambdaQueryWrapper<MisSettleList>()
                .eq(MisSettleList::getBillId, bill.getId())
                .ne(MisSettleList::getCheckStatus, 3));
        if (exists != null && exists > 0) {
            return ActionVO.fail("该结算单已生成结算清单，未重复生成");
        }

        MisSettleList l = new MisSettleList();
        l.setListNo("QL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%06d", SEQ.incrementAndGet()));
        l.setBillId(bill.getId());
        l.setBillNo(bill.getBillNo());
        l.setPatientName(bill.getPatientName());
        l.setIdCardMask(bill.getIdCardMask());
        l.setInsuranceType(bill.getInsuranceType());
        l.setEncounterType(bill.getSettleType());
        l.setItemCount(items == null ? 0 : items.size());
        l.setTotalAmount(bill.getTotalAmount() == null ? BigDecimal.ZERO : bill.getTotalAmount());
        l.setInsurancePay(bill.getInsurancePay() == null ? BigDecimal.ZERO : bill.getInsurancePay());
        l.setListVersion("1.0");
        l.setUploadBatchNo(batchNo());
        l.setCheckStatus(0);
        l.setUploadTime(LocalDateTime.now());
        listMapper.insert(l);
        return ActionVO.ok();
    }

    @Override
    public ActionVO voidList(Long billId, String reason) {
        if (billId == null) {
            return ActionVO.fail("结算单ID不能为空");
        }
        MisSettleList l = listMapper.selectOne(new LambdaQueryWrapper<MisSettleList>()
                .eq(MisSettleList::getBillId, billId)
                .ne(MisSettleList::getCheckStatus, 3)
                .orderByDesc(MisSettleList::getId)
                .last("limit 1"));
        if (l == null) {
            return ActionVO.fail("未找到该结算单的有效清单");
        }
        l.setCheckStatus(3);
        listMapper.updateById(l);
        return ActionVO.ok();
    }

    @Override
    public void updateCheckStatus(Long billId, Integer checkStatus) {
        if (billId == null || checkStatus == null) {
            return;
        }
        MisSettleList l = listMapper.selectOne(new LambdaQueryWrapper<MisSettleList>()
                .eq(MisSettleList::getBillId, billId)
                .ne(MisSettleList::getCheckStatus, 3)
                .orderByDesc(MisSettleList::getId)
                .last("limit 1"));
        if (l != null) {
            l.setCheckStatus(checkStatus);
            listMapper.updateById(l);
        }
    }

    @Override
    public SettleListVO detail(Long id) {
        MisSettleList l = listMapper.selectById(id);
        return l == null ? null : toVO(l);
    }

    /** 上传批次号：按天分组，同一天的清单归入同一批次 */
    private String batchNo() {
        return "PC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private SettleListVO toVO(MisSettleList e) {
        SettleListVO vo = new SettleListVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
