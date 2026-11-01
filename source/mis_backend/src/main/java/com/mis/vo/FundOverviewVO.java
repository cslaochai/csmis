package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 基金运行分析总览
 * <p>口径说明：
 * <ul>
 *   <li>基金统筹收入 = 统筹支付 + 个人账户支付（两者均来源于参保缴费）；</li>
 *   <li>基金支出 = 月度结算已拨付金额合计；</li>
 *   <li>基金率 = 基金支出 / 基金统筹收入；</li>
 *   <li>结算总量 = 经办结算单 + 机构报盘结算单。</li>
 * </ul>
 */
@Data
public class FundOverviewVO implements Serializable {

    /** 统计区间（天） */
    private Integer rangeDays;
    /** 统计起始日 */
    private String startDate;
    /** 统计截止日 */
    private String endDate;

    /** 参保人总数 */
    private Integer insuredCount;
    /** 正常参保人数 */
    private Integer activeInsuredCount;
    /** 停保人数 */
    private Integer suspendedInsuredCount;

    /** 结算总笔数 */
    private Integer settleCount;
    /** 总医疗费用 */
    private BigDecimal totalCost;
    /** 统筹基金支付 */
    private BigDecimal insurancePay;
    /** 个人账户支付 */
    private BigDecimal personalAccount;
    /** 个人现金支付 */
    private BigDecimal cashPay;
    /** 个人自付合计 */
    private BigDecimal selfPay;

    /** 基金统筹收入 */
    private BigDecimal fundIncome;
    /** 基金支出（已拨付） */
    private BigDecimal fundExpense;
    /** 基金结余 */
    private BigDecimal fundBalance;
    /** 基金率（%） */
    private BigDecimal fundRate;
    /** 基金支付占医疗费用比例（%） */
    private BigDecimal payRatio;
    /** 次均医疗费用 */
    private BigDecimal avgCost;
    /** 人均医疗费用 */
    private BigDecimal perCapitaCost;
    /** 人均基金支出 */
    private BigDecimal perCapitaExpense;
}
