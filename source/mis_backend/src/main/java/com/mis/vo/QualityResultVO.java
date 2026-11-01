package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QualityResultVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long billId;
    private String billNo;
    private Long itemId;
    private String itemName;
    private String ybCode;
    private String medinsCode;
    private String medinsName;
    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private Integer severity;
    private String fieldName;
    private String originValue;
    private String qualityDesc;
    private BigDecimal involveAmount;
    private BigDecimal deductAmount;
    private Integer handleStatus;
    private String handleOpinion;
    private String handleUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkTime;
}
