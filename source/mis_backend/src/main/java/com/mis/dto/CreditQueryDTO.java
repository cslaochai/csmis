package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 信用管理分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreditQueryDTO extends BizPageParam {

    private Integer subjectType;
    private Integer punishType;
}
