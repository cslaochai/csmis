package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TransferQueryDTO extends BizPageParam {

    private String keyword;
    /** 转移类型（1-省内转移 2-跨省转移 3-转出 4-转入） */
    private Integer transferType;
    private Integer status;
}
