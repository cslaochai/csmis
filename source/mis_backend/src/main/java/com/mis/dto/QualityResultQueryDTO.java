package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QualityResultQueryDTO extends BizPageParam {

    private String keyword;
    /** 规则编码 */
    private String ruleCode;
    /** 严重程度 */
    private Integer severity;
    /** 处理状态（0-待处理 1-已确认 2-已整改 3-已忽略） */
    private Integer handleStatus;
    /** 结算单号 */
    private String billNo;
}
