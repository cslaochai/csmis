package com.mis.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 追溯码核验统计
 */
@Data
public class TraceStatsVO implements Serializable {

    /** 台账码总数 */
    private Integer totalCodes;
    /** 未使用 */
    private Integer unusedCodes;
    /** 已使用 */
    private Integer usedCodes;
    /** 已作废 */
    private Integer voidCodes;
    /** 核验总次数 */
    private Integer verifyCount;
    /** 核验通过 */
    private Integer passCount;
    /** 异常核验 */
    private Integer abnormalCount;
    /** 追溯码不存在 */
    private Integer missingCount;
    /** 重复使用 */
    private Integer reusedCount;
    /** 与项目不符 */
    private Integer mismatchCount;
    /** 已过期 */
    private Integer expiredCount;
}
