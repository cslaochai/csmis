package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 月度拨付分析
 */
@Data
public class MonthlyAllocStatVO implements Serializable {

    /** 账期（yyyy-MM） */
    private String month;
    /** 申报笔数 */
    private Integer declareCount;
    /** 申报金额 */
    private BigDecimal declareAmount;
    /** 扣减金额 */
    private BigDecimal deductAmount;
    /** 拨付金额 */
    private BigDecimal allocatedAmount;
    /** 扣减率（%） */
    private BigDecimal deductRate;
    /** 状态 */
    private Integer status;
}
