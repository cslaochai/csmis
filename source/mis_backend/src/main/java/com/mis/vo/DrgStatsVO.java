package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DRG/DIP 支付监管看板统计
 */
@Data
public class DrgStatsVO implements Serializable {

    /** 已入组病例数 */
    private Integer totalCases;
    /** 偏差预警数 */
    private Integer warnCases;
    /** 费用超支数 */
    private Integer overCases;
    /** 特例单议数 */
    private Integer specialCases;
    /** 涉及费用合计 */
    private BigDecimal totalCost;
    /** 支付标准合计 */
    private BigDecimal totalEstCost;
    /** 偏差率合计（%） */
    private BigDecimal deviationPercent;
    /** 病组/病种池数量 */
    private Integer groupCount;
}
