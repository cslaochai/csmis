package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 基金监管成效分析
 */
@Data
public class SupervisionEffectVO implements Serializable {

    /* ---------------- 智能审核 ---------------- */
    /** 审核任务总数 */
    private Integer auditTotal;
    /** 已通过 */
    private Integer auditPassed;
    /** 已驳回 */
    private Integer auditRejected;
    /** 申诉中 */
    private Integer auditAppealing;
    /** 已合议 */
    private Integer auditConferred;
    /** 审核通过率（%） */
    private BigDecimal auditPassRate;
    /** 涉及金额 */
    private BigDecimal auditAmount;

    /* ---------------- 疑点稽核 ---------------- */
    /** 疑点总数 */
    private Integer suspicionTotal;
    /** 待处理 */
    private Integer suspicionPending;
    /** 已确认 */
    private Integer suspicionConfirmed;
    /** 已排除 */
    private Integer suspicionExcluded;
    /** 疑点涉及金额 */
    private BigDecimal suspicionAmount;

    /* ---------------- 大数据反欺诈 ---------------- */
    /** 线索总数 */
    private Integer clueTotal;
    /** 待核查 */
    private Integer cluePending;
    /** 已确认 */
    private Integer clueConfirmed;
    /** 已排除 */
    private Integer clueExcluded;
    /** 已移送 */
    private Integer clueTransferred;

    /* ---------------- 事前提醒/事中拦截 ---------------- */
    /** 事前/事中命中数 */
    private Integer precheckTotal;
    /** 事中阻断数 */
    private Integer precheckBlocked;
    /** 待处理 */
    private Integer precheckPending;

    /* ---------------- 结算清单质控 ---------------- */
    /** 质控命中数 */
    private Integer qualityTotal;
    /** 严重级 */
    private Integer qualitySerious;
    /** 待处理 */
    private Integer qualityPending;
    /** 建议扣款合计 */
    private BigDecimal qualityDeduct;

    /* ---------------- 药品追溯码 ---------------- */
    /** 核验次数 */
    private Integer traceTotal;
    /** 核验通过 */
    private Integer tracePassed;
    /** 异常核验 */
    private Integer traceAbnormal;

    /* ---------------- 违规追回 ---------------- */
    /** 追回单数 */
    private Integer recoverTotal;
    /** 追回金额 */
    private BigDecimal recoverAmount;
    /** 待追回 */
    private Integer recoverPending;

    /* ---------------- 自查自纠 ---------------- */
    /** 任务总数 */
    private Integer selfCheckTotal;
    /** 已办结 */
    private Integer selfCheckDone;
    /** 办结率（%） */
    private BigDecimal selfCheckDoneRate;
}
