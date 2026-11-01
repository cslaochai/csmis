package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RandomTaskQueryDTO extends BizPageParam {

    private String keyword;
    /** 检查类型（1-日常检查 2-专项检查 3-监督抽查） */
    private Integer checkType;
    private Integer status;
}
