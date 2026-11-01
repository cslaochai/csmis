package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 结算清单质控看板统计
 */
@Data
public class QualityStatsVO implements Serializable {

    /** 质控结算单数 */
    private Integer checkedBills;
    /** 质控明细条数 */
    private Integer checkedItems;
    /** 命中质控项数 */
    private Integer hitCount;
    /** 提示级 */
    private Integer tipCount;
    /** 警示级 */
    private Integer warnCount;
    /** 严重级 */
    private Integer seriousCount;
    /** 待处理 */
    private Integer pendingCount;
    /** 涉及金额合计 */
    private BigDecimal involveAmount;
    /** 建议扣款合计 */
    private BigDecimal deductAmount;
    /** 规则数 */
    private Integer ruleCount;
}
