package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 维度聚合（险种结构 / 费用构成通用）
 */
@Data
public class DimensionStatVO implements Serializable {

    /** 维度值（险种 / 就诊类型 / 支付类别 / 项目类别） */
    private String name;
    /** 结算笔数 */
    private Integer count;
    /** 医疗费用 */
    private BigDecimal amount;
    /** 统筹支付 */
    private BigDecimal insurancePay;
    /** 占医疗费用比例（%） */
    private BigDecimal rate;
    /** 次均费用 */
    private BigDecimal avgAmount;
}
