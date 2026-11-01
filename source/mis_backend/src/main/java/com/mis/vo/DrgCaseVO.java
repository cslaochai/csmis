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
public class DrgCaseVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String caseNo;
    private Long billId;
    private String billNo;
    private String patientName;
    private String idCardMask;
    private String insuranceType;
    private Integer payType;
    private String groupCode;
    private String groupName;
    private BigDecimal weight;
    private BigDecimal estCost;
    private BigDecimal actualCost;
    private BigDecimal deviationRate;
    private Integer status;
    private String deviationReason;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate caseDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
