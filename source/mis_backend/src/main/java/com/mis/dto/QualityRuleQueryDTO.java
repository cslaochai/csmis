package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QualityRuleQueryDTO extends BizPageParam {

    private String keyword;
    /** 校验类型 */
    private String ruleType;
    private Integer severity;
    private Integer status;
}
