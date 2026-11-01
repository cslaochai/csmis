package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 手工报销分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClaimQueryDTO extends BizPageParam {

    private Integer claimType;
    private Integer status;
}
