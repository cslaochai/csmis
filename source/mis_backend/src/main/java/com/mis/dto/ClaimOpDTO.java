package com.mis.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 手工报销审核 / 拨付入参
 */
@Data
public class ClaimOpDTO implements Serializable {

    private Long id;
    /** 审核核定的统筹支付额（审核通过时必填） */
    private BigDecimal insurancePay;
    /** 审核意见 */
    private String auditOpinion;
    /** 是否通过（true-通过并进入待拨付；false-退回） */
    private Boolean passed;
}
