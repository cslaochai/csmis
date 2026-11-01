package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PrecheckRuleQueryDTO extends BizPageParam {

    private String keyword;
    private String triggerStage;
    private Integer status;
}
