package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.TransferHandleDTO;
import com.mis.dto.TransferQueryDTO;
import com.mis.dto.TransferUpsertDTO;
import com.mis.entity.MisInsuredPerson;
import com.mis.entity.MisAccountTxn;
import com.mis.entity.MisTransfer;
import com.mis.mapper.MisInsuredPersonMapper;
import com.mis.mapper.MisAccountTxnMapper;
import com.mis.mapper.MisTransferMapper;
import com.mis.service.TransferService;
import com.mis.vo.ActionVO;
import com.mis.vo.TransferVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 医保关系转移接续：受理 → 转出 → 转入 → 完成（缴费年限核定 + 个人账户结转）
 */
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final MisTransferMapper mapper;
    private final MisInsuredPersonMapper personMapper;
    private final MisAccountTxnMapper accountTxnMapper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Override
    public PageVO<TransferVO> page(TransferQueryDTO q) {
        Page<MisTransfer> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisTransfer> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MisTransfer::getTransferNo, k).or().like(MisTransfer::getPersonName, k)
                    .or().like(MisTransfer::getIdCard, k).or().like(MisTransfer::getToRegion, k));
        }
        if (q.getTransferType() != null) {
            w.eq(MisTransfer::getTransferType, q.getTransferType());
        }
        if (q.getStatus() != null) {
            w.eq(MisTransfer::getStatus, q.getStatus());
        }
        w.orderByDesc(MisTransfer::getId);
        Page<MisTransfer> res = mapper.selectPage(page, w);
        List<TransferVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsert(TransferUpsertDTO dto) {
        MisInsuredPerson person = personMapper.selectOne(new LambdaQueryWrapper<MisInsuredPerson>()
                .eq(MisInsuredPerson::getIdCard, dto.getIdCard())
                .last("limit 1"));
        if (person == null) {
            return ActionVO.fail("参保人未在参保库登记，无法发起转移接续");
        }
        MisTransfer t;
        if (dto.getId() == null) {
            t = new MisTransfer();
            t.setTransferNo("ZY" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%05d", SEQ.incrementAndGet()));
            t.setStatus(1);
        } else {
            t = mapper.selectById(dto.getId());
            if (t == null) {
                return ActionVO.fail("转移单不存在");
            }
            if (t.getStatus() != null && t.getStatus() >= 4) {
                return ActionVO.fail("该转移单已进入结转环节，不能修改");
            }
        }
        t.setPersonName(dto.getPersonName());
        t.setIdCard(dto.getIdCard());
        t.setInsuranceNo(dto.getInsuranceNo() != null ? dto.getInsuranceNo() : person.getInsuranceNo());
        t.setTransferType(dto.getTransferType());
        t.setFromRegion(dto.getFromRegion() != null ? dto.getFromRegion() : person.getRegion());
        t.setToRegion(dto.getToRegion());
        t.setApplyDate(dto.getApplyDate() != null ? dto.getApplyDate() : LocalDate.now());
        t.setPayYears(dto.getPayYears());
        t.setRemark(dto.getRemark());
        t.setUpdateTime(LocalDateTime.now());
        if (dto.getId() == null) {
            t.setCreateTime(LocalDateTime.now());
            mapper.insert(t);
        } else {
            mapper.updateById(t);
        }
        return ActionVO.ok();
    }

    /**
     * 转移处置。完成转移（action=5）时：
     * 1) 核定并结转缴费年限；
     * 2) 结转个人账户余额（生成转移出/入流水，扣减转出方余额）。
     */
    @Override
    public ActionVO handle(TransferHandleDTO dto) {
        MisTransfer t = mapper.selectById(dto.getId());
        if (t == null) {
            return ActionVO.fail("转移单不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        int action = dto.getAction();
        if (action == 2) {
            if (t.getStatus() != null && t.getStatus() != 1) {
                return ActionVO.fail("仅「待受理」的转移单可受理");
            }
            t.setStatus(2);
        } else if (action == 3) {
            if (t.getStatus() == null || t.getStatus() != 2) {
                return ActionVO.fail("仅「已受理」的转移单可办理转出");
            }
            t.setStatus(3);
        } else if (action == 4) {
            if (t.getStatus() == null || t.getStatus() != 3) {
                return ActionVO.fail("仅「已转出」的转移单可办理转入");
            }
            t.setStatus(4);
        } else if (action == 5) {
            if (t.getStatus() == null || t.getStatus() != 4) {
                return ActionVO.fail("仅「已转入」的转移单可完成转移");
            }
            MisInsuredPerson person = personMapper.selectOne(new LambdaQueryWrapper<MisInsuredPerson>()
                    .eq(MisInsuredPerson::getIdCard, t.getIdCard())
                    .last("limit 1"));
            if (person == null) {
                return ActionVO.fail("参保人不存在，无法结转个人账户");
            }
            BigDecimal balance = person.getAccountBalance() == null ? BigDecimal.ZERO : person.getAccountBalance();
            t.setFundBalance(balance);
            // 个人账户结转：转出方余额清零，生成转移出流水
            person.setAccountBalance(BigDecimal.ZERO);
            personMapper.updateById(person);
            writeTxn(t, person, 4, balance.negate(), BigDecimal.ZERO, "医保关系转移接续结转个人账户");
            t.setStatus(5);
            t.setTransferTime(now);
        } else {
            return ActionVO.fail("不支持的处置动作");
        }
        if (dto.getPayYears() != null) {
            t.setPayYears(dto.getPayYears());
        }
        t.setHandler(UserContext.username());
        t.setHandleOpinion(dto.getOpinion());
        t.setUpdateTime(now);
        mapper.updateById(t);
        return ActionVO.ok();
    }

    private void writeTxn(MisTransfer t, MisInsuredPerson person, int txnType, BigDecimal amount,
                          BigDecimal balanceAfter, String reason) {
        MisAccountTxn txn = new MisAccountTxn();
        txn.setTxnNo("ZH" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SEQ.incrementAndGet()));
        txn.setIdCard(t.getIdCard());
        txn.setPersonName(person.getName());
        txn.setTxnType(txnType);
        txn.setAmount(amount);
        txn.setBalanceAfter(balanceAfter);
        txn.setTxnTime(LocalDateTime.now());
        txn.setReason(reason);
        txn.setRefNo(t.getTransferNo());
        txn.setStatus(1);
        txn.setCreateTime(LocalDateTime.now());
        accountTxnMapper.insert(txn);
    }

    private TransferVO toVO(MisTransfer t) {
        TransferVO vo = new TransferVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }
}
