package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 手工报销出参
 */
@Data
public class ClaimVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String claimNo;
    private String personName;
    private String idCard;
    private String insuranceType;
    private Integer claimType;
    private LocalDate visitDate;
    private BigDecimal totalAmount;
    private BigDecimal insurancePay;
    private BigDecimal personalPay;
    private Integer status;
    private String applyReason;
    private String auditOpinion;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
