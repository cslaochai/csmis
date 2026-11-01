package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 基金运行趋势点（按日）
 */
@Data
public class FundTrendVO implements Serializable {

    /** 日期 */
    private String day;
    /** 结算笔数 */
    private Integer count;
    /** 医疗费用 */
    private BigDecimal amount;
    /** 统筹支付 */
    private BigDecimal insurancePay;
    /** 个人账户支付 */
    private BigDecimal personalAccount;
    /** 个人现金支付 */
    private BigDecimal cashPay;
    /** 日拨付金额 */
    private BigDecimal allocAmount;
}
