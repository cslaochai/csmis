package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定点协议出参
 */
@Data
public class AgreementVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String medinsCode;
    private String medinsName;
    private Integer medinsType;
    private String agreementNo;
    private Integer agreementStatus;
    private LocalDate signDate;
    private LocalDate expireDate;
    private String serviceScope;
    private Integer annualScore;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
