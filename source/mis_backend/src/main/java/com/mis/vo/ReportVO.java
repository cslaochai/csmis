package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReportVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String reportNo;
    private String reporterName;
    private String reporterContact;
    private String reportedMedinsName;
    private String reportedSubject;
    private String problemType;
    private String content;
    private Integer status;
    private String handleUser;
    private String handleOpinion;
    private BigDecimal rewardAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
