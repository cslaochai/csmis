package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QualityRuleVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private Integer severity;
    private BigDecimal deductRatio;
    private Integer deductType;
    private BigDecimal deductAmount;
    private String params;
    private Integer status;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
