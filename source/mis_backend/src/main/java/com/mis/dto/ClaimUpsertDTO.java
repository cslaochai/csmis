package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 手工报销申请入参（受理登记）
 */
@Data
public class ClaimUpsertDTO implements Serializable {

    private Long id;
    @NotBlank(message = "姓名不能为空")
    private String personName;
    @NotBlank(message = "身份证号不能为空")
    private String idCard;
    /** 险种（2-城镇职工医保 3-城乡居民医保） */
    private String insuranceType;
    /** 报销类型（1-门诊 2-住院 3-药店） */
    private Integer claimType;
    private LocalDate visitDate;
    private BigDecimal totalAmount;
    private BigDecimal personalPay;
    private String applyReason;
    private String remark;
}
