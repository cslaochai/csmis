package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TransferVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String transferNo;
    private String personName;
    private String idCard;
    private String insuranceNo;
    private Integer transferType;
    private String fromRegion;
    private String toRegion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyDate;
    private Integer status;
    private BigDecimal payYears;
    private BigDecimal fundBalance;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transferTime;
    private String handler;
    private String handleOpinion;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
