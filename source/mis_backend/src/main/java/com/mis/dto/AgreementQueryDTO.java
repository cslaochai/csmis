package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定点协议分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgreementQueryDTO extends BizPageParam {

    private Integer medinsType;
    private Integer agreementStatus;
}
