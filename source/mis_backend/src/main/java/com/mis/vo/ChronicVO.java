package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ChronicVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String recordNo;
    private String personName;
    private String idCard;
    private String insuranceNo;
    private String diseaseCode;
    private String diseaseName;
    private Integer diseaseLevel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyDate;
    private String doctor;
    private String hospital;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validFrom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validTo;
    private String reviewer;
    private String reviewOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
