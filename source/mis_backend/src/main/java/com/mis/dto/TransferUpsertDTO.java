package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransferUpsertDTO {

    private Long id;
    @NotBlank(message = "参保人姓名不能为空")
    private String personName;
    @NotBlank(message = "身份证号不能为空")
    private String idCard;
    private String insuranceNo;
    /** 转移类型（1-省内转移 2-跨省转移 3-转出 4-转入） */
    @NotNull(message = "转移类型不能为空")
    private Integer transferType = 1;
    private String fromRegion;
    private String toRegion;
    private LocalDate applyDate;
    /** 累计缴费年限（核定，年） */
    private BigDecimal payYears;
    private String remark;
}
