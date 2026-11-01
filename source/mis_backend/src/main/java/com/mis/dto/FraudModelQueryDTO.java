package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FraudModelQueryDTO extends BizPageParam {

    private String keyword;
    private String category;
    private Integer status;
}
