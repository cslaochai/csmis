package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 事前提醒 / 事中拦截 评估结论
 * <p>pass=false 表示存在阻断项（blockFlag=1），结算关口应拒绝；否则提示经办注意。
 */
@Data
public class PrecheckVO implements Serializable {

    /** 是否可继续（无阻断项） */
    private boolean pass;
    /** 是否存在阻断项（1-有 0-无） */
    private Integer blockFlag;
    /** 命中条数 */
    private Integer hitCount;
    /** 最高风险等级（1-低 2-中 3-高，无命中为 0） */
    private Integer maxRiskLevel;
    /** 处置建议（放行 / 提醒后放行 / 阻断结算 / 转人工复核） */
    private String suggest;
    /** 命中明细 */
    private List<PrecheckResultVO> hits;
}
