package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DrgGroupQueryDTO extends BizPageParam {

    private String keyword;
    /** 支付方式（1-DRG 2-DIP） */
    private Integer payType;
    private Integer status;
}
