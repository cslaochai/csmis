package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChronicQueryDTO extends BizPageParam {

    private String keyword;
    /** 待遇类别（1-门诊特殊病种 2-门诊慢特病） */
    private Integer diseaseLevel;
    private Integer status;
}
