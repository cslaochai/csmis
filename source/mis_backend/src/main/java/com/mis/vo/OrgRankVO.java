package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 定点机构结算排名
 */
@Data
public class OrgRankVO implements Serializable {

    /** 排名 */
    private Integer rank;
    /** 机构编码 */
    private String medinsCode;
    /** 机构名称 */
    private String medinsName;
    /** 结算笔数 */
    private Integer settleCount;
    /** 医疗费用 */
    private BigDecimal totalAmount;
    /** 统筹支付 */
    private BigDecimal insurancePay;
    /** 违规扣减金额 */
    private BigDecimal deductAmount;
    /** 违规扣减笔数 */
    private Integer deductCount;
    /** 次均费用 */
    private BigDecimal avgAmount;
}
