package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 医保费用分解（试算）结果
 */
@Data
public class DecVO implements Serializable {

    private BigDecimal total;
    private BigDecimal insurancePay;
    private BigDecimal personalAccount;
    private BigDecimal cashPay;
    private BigDecimal selfPay;
    private BigDecimal deductible;
    private BigDecimal basePool;

    /** 事前提醒命中明细（第一道防线，结算前实时比对） */
    private List<PrecheckResultVO> precheckList;
    /** 事前提醒是否存在阻断项（1-有 0-无） */
    private Integer precheckBlockFlag;
}
