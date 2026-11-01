package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FraudClueVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String clueNo;
    private Long modelId;
    private String modelCode;
    private String modelName;
    private String medinsCode;
    private String medinsName;
    private String insuredIdCard;
    private String insuredName;
    private String bizNo;
    private BigDecimal involveAmount;
    private Integer riskLevel;
    private String clueDetail;
    private Integer status;
    private String handleUser;
    private String handleOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
