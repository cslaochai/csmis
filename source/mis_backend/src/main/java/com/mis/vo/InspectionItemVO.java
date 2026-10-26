package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 飞检发现问题明细出参
 */
@Data
public class InspectionItemVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inspectionId;
    /** 问题类型 */
    private String problemType;
    /** 问题描述 */
    private String problemDesc;
    /** 证据说明 */
    private String evidence;
    /** 涉及金额 */
    private BigDecimal involvedAmount;
    /** 处置方式（整改/追回/移交稽核） */
    private String dispose;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
