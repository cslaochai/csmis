package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PrecheckResultQueryDTO extends BizPageParam {

    private String keyword;
    private String triggerStage;
    private Integer handleStatus;
}
