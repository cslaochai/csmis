package com.mis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * 医疗救助报销
 */
@Data
@TableName("mis_aid_claim")
public class MisAidClaim implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 报销单号 */
    private String claimNo;

    /** 身份证号 */
    private String idCard;

    /** 姓名 */
    private String personName;

    /** 救助类型 */
    private Integer aidType;

    /** 医疗费用总额 */
    private BigDecimal totalAmount;

    /** 医保支付金额 */
    private BigDecimal insurancePay;

    /** 救助金额 */
    private BigDecimal aidAmount;

    /** 个人自付 */
    private BigDecimal selfPay;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 申请日期 */
    private LocalDate claimDate;

    /** 状态（0-待审核 1-已审核 2-已支付 3-已驳回） */
    private Integer status;

    /** 审核人 */
    private String reviewer;

    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createById;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateById;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}