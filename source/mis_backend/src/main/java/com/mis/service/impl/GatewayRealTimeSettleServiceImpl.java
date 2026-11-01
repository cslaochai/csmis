package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mis.dto.GatewayRealTimeSettleDTO;
import com.mis.engine.SettleEngine;
import com.mis.entity.MisInsuredPerson;
import com.mis.entity.MisMessage;
import com.mis.entity.MisSettlement;
import com.mis.entity.MisFeeItem;
import com.mis.mapper.MisInsuredPersonMapper;
import com.mis.mapper.MisMessageMapper;
import com.mis.mapper.MisSettlementMapper;
import com.mis.service.GatewayRealTimeSettleService;
import com.mis.vo.DecVO;
import com.mis.vo.GatewaySettleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 网关实时结算实现。
 *
 * <p>2201 与 2301 共用同一套「查参保 → 费用分解」，2301 额外落库 + 扣个账。
 * 个账只对职工医保扣（居民医保没有个人账户，personalAccount 恒为 0，SettleEngine 已处理）。</p>
 */
@Service
@RequiredArgsConstructor
public class GatewayRealTimeSettleServiceImpl implements GatewayRealTimeSettleService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MisInsuredPersonMapper personMapper;
    private final MisSettlementMapper settlementMapper;
    private final MisMessageMapper messageMapper;
    private final SettleEngine settleEngine;

    @Override
    public GatewaySettleVO preSettle(GatewayRealTimeSettleDTO dto) {
        return doSettle(dto, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GatewaySettleVO formalSettle(GatewayRealTimeSettleDTO dto) {
        // 幂等：同一 tradeNo 已正式结算过，直接回原结果
        MisSettlement exist = byTradeNo(dto.getTradeNo());
        if (exist != null && exist.getStatus() != null && exist.getStatus() == 1) {
            return toVO(exist, null, "重复结算，按原结果幂等返回");
        }
        return doSettle(dto, true);
    }

    // ------------------------------------------------------------------

    private GatewaySettleVO doSettle(GatewayRealTimeSettleDTO dto, boolean persist) {
        GatewaySettleVO vo = new GatewaySettleVO();
        vo.setTradeNo(dto.getTradeNo());

        // 1) 必填入参
        if (!StringUtils.hasText(dto.getTradeNo())) {
            return fail(vo, "缺少 tradeNo");
        }
        if (!StringUtils.hasText(dto.getIdCard()) && !StringUtils.hasText(dto.getInsuranceNo())) {
            return fail(vo, "必须传 idCard 或 insuranceNo");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return fail(vo, "费用明细为空");
        }

        // 2) 查参保人（按身份证优先，与 /insure/person/query 同口径）
        MisInsuredPerson person = findPerson(dto);
        if (person == null) {
            return fail(vo, "参保库查无此人（idCard=" + dto.getIdCard() + "，insuranceNo=" + dto.getInsuranceNo() + "）");
        }
        vo.setName(person.getName());
        vo.setInsuranceType(person.getInsuranceType());
        if (person.getPersonStatus() == null || person.getPersonStatus() != 1) {
            return fail(vo, "参保人 " + person.getName() + " 当前为停保/异常状态，不能联网结算");
        }

        // 3) 费用分解
        List<MisFeeItem> engineItems = new ArrayList<>(dto.getItems().size());
        for (GatewayRealTimeSettleDTO.Item it : dto.getItems()) {
            MisFeeItem m = new MisFeeItem();
            m.setName(it.getName());
            m.setYbCode(it.getYbCode());
            m.setPayClass(it.getPayClass());
            m.setSelfRatio(it.getSelfRatio() == null ? BigDecimal.ZERO : it.getSelfRatio());
            m.setPrice(it.getPrice());
            m.setQty(it.getQty() == null ? 1 : it.getQty());
            engineItems.add(m);
        }
        DecVO d = settleEngine.compute(engineItems, person.getInsuranceType());

        vo.setTotal(d.getTotal());
        vo.setInsurancePay(d.getInsurancePay());
        vo.setPersonalAccount(d.getPersonalAccount());
        vo.setCashPay(d.getCashPay());
        vo.setSelfPay(d.getSelfPay());
        vo.setDeductible(d.getDeductible());

        // 4) 2301 才落库 + 扣个账
        if (!persist) {
            vo.setInfcode(0);
            vo.setAccountBalanceAfter(person.getAccountBalance());
            return vo;
        }

        // 个账余额校验（职工个账不够扣时按余额封顶，不硬拦——真实医保也是个账优先、不够的转现金）
        BigDecimal accountBefore = person.getAccountBalance() == null ? BigDecimal.ZERO : person.getAccountBalance();
        BigDecimal accountToDeduct = d.getPersonalAccount().min(accountBefore).max(BigDecimal.ZERO);
        BigDecimal accountAfter = accountBefore.subtract(accountToDeduct);
        // 个账扣不足的部分转现金
        BigDecimal cashPayFinal = d.getCashPay().add(d.getPersonalAccount().subtract(accountToDeduct));

        // 扣个账
        person.setAccountBalance(accountAfter);
        personMapper.updateById(person);

        // 落 mis_settlement
        MisSettlement s = new MisSettlement();
        s.setTradeNo(dto.getTradeNo());
        s.setSettlementNo(dto.getTradeNo());
        s.setBillNo(dto.getBillNo());
        s.setEncounterType(dto.getEncounterType());
        s.setPatientName(person.getName());
        s.setIdCard(person.getIdCard());
        s.setInsuranceNo(person.getInsuranceNo());
        s.setInsuranceType(person.getInsuranceType());
        s.setTotalAmount(scale(d.getTotal()));
        s.setInsurancePay(scale(d.getInsurancePay()));
        s.setPersonalPay(scale(accountToDeduct.add(cashPayFinal)));
        s.setSelfPay(scale(d.getSelfPay()));
        s.setPoolConfirm(scale(d.getInsurancePay()));
        s.setAccountDeduct(accountToDeduct);
        s.setStatus(1);
        s.setBillDate(LocalDate.now());
        s.setWarnMsg(accountToDeduct.compareTo(d.getPersonalAccount()) < 0
                ? "个账余额不足，缺口 ¥" + d.getPersonalAccount().subtract(accountToDeduct) + " 已转现金收取" : null);
        settlementMapper.insert(s);

        String receiptNo = "YBZ" + LocalDate.now().format(DAY) + String.format("%06d", s.getId());
        s.setReceiptNo(receiptNo);
        settlementMapper.updateById(s);

        // 报文台账
        MisMessage msg = new MisMessage();
        msg.setMsgType("2301");
        msg.setTradeNo(dto.getTradeNo());
        msg.setSuccess(1);
        msg.setReceiptNo(receiptNo);
        msg.setPayload("real-time settle: " + person.getName() + " total=" + d.getTotal()
                + " insurancePay=" + d.getInsurancePay() + " account=" + accountToDeduct + " cash=" + cashPayFinal);
        messageMapper.insert(msg);

        vo.setInfcode(0);
        vo.setReceiptNo(receiptNo);
        vo.setSettlementNo(dto.getTradeNo());
        vo.setPersonalAccount(scale(accountToDeduct));
        vo.setCashPay(scale(cashPayFinal));
        vo.setAccountBalanceAfter(scale(accountAfter));
        return vo;
    }

    private MisInsuredPerson findPerson(GatewayRealTimeSettleDTO dto) {
        LambdaQueryWrapper<MisInsuredPerson> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getIdCard())) {
            w.eq(MisInsuredPerson::getIdCard, dto.getIdCard());
        } else {
            w.eq(MisInsuredPerson::getInsuranceNo, dto.getInsuranceNo());
        }
        w.last("limit 1");
        return personMapper.selectOne(w);
    }

    private MisSettlement byTradeNo(String tradeNo) {
        if (!StringUtils.hasText(tradeNo)) {
            return null;
        }
        return settlementMapper.selectOne(new LambdaQueryWrapper<MisSettlement>()
                .eq(MisSettlement::getTradeNo, tradeNo)
                .orderByDesc(MisSettlement::getId)
                .last("limit 1"));
    }

    private GatewaySettleVO toVO(MisSettlement s, BigDecimal accountAfter, String warn) {
        GatewaySettleVO vo = new GatewaySettleVO();
        vo.setInfcode(0);
        vo.setTradeNo(s.getTradeNo());
        vo.setReceiptNo(s.getReceiptNo());
        vo.setSettlementNo(s.getSettlementNo());
        vo.setInsuranceType(s.getInsuranceType());
        vo.setName(s.getPatientName());
        vo.setTotal(s.getTotalAmount());
        vo.setInsurancePay(s.getInsurancePay());
        vo.setSelfPay(s.getSelfPay());
        vo.setAccountBalanceAfter(accountAfter);
        vo.setWarnMsg(warn);
        return vo;
    }

    private GatewaySettleVO fail(GatewaySettleVO vo, String errMsg) {
        vo.setInfcode(-1);
        vo.setErrMsg(errMsg);
        return vo;
    }

    private BigDecimal scale(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : v.setScale(2, RoundingMode.HALF_UP);
    }
}
