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
 * 参保关系变更历史
 */
@Data
@TableName("mis_insure_change")
public class MisInsureChange implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 身份证号 */
    private String idCard;

    /** 参保人姓名 */
    private String personName;

    /** 变更类型（参保/停保/恢复/终止/转移/信息变更） */
    private String changeType;

    /** 变更前值 */
    private String beforeValue;

    /** 变更后值 */
    private String afterValue;

    /** 变更原因 */
    private String changeReason;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 经办人 */
    private String operator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 经办时间 */
    private LocalDateTime operTime;

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