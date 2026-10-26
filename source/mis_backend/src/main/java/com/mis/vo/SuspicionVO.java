package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 疑点记录出参：疑点稽核列表行与详情。
 */
@Data
public class SuspicionVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 疑点单号（唯一） */
    private String suspicionNo;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ruleId;
    /** 命中规则名称 */
    private String ruleName;
    /** 规则类型 */
    private String ruleType;
    /** 疑点来源（1-经办结算单 2-机构报盘） */
    private Integer bizSource;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizId;
    /** 来源单据号 */
    private String bizNo;
    private String patientName;
    /** 脱敏身份证号 */
    private String idCardMask;
    /** 参保类型 */
    private String insuranceType;
    /** 就诊类型（门诊/住院） */
    private String encounterType;
    private String dept;
    private String doctor;
    /** 业务日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bizDate;
    /** 疑点涉及金额 */
    private BigDecimal involvedAmount;
    /** 疑点说明 */
    private String reason;
    /** 状态（待核实/已确认/已排除） */
    private String status;
    /** 确认违规金额 */
    private BigDecimal confirmAmount;
    /** 处置人 */
    private String handleUser;
    /** 处置时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    /** 备注 */
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
