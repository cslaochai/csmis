package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FraudClueQueryDTO extends BizPageParam {

    private String keyword;
    private Integer status;
    private Integer riskLevel;
}
