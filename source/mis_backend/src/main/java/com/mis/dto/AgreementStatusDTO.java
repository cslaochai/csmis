package com.mis.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 定点协议状态变更（签订 / 解除）
 */
@Data
public class AgreementStatusDTO implements Serializable {

    private Long id;
    /** 目标状态（1-已签订 2-已解除） */
    private Integer agreementStatus;
    private String remark;
}
