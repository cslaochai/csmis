package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PrecheckResultVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private String triggerStage;
    private String bizNo;
    private String medinsCode;
    private String medinsName;
    private String insuredIdCard;
    private String insuredName;
    private String promptText;
    private Integer blockFlag;
    private Integer riskLevel;
    private Integer handleStatus;
    private String handleOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime hitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
