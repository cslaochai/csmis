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
 * 医保关系转移接续（转出 / 转入 / 缴费年限与个人账户结转）
 */
@Data
@TableName("mis_transfer")
public class MisTransfer implements Serializable {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 转移单号（唯一） */
    private String transferNo;
    private String personName;
    private String idCard;
    private String insuranceNo;
    /** 转移类型（1-省内转移 2-跨省转移 3-转出 4-转入） */
    private Integer transferType;
    /** 转出统筹区 */
    private String fromRegion;
    /** 转入统筹区 */
    private String toRegion;
    /** 申请日期 */
    private LocalDate applyDate;
    /** 状态（1-待受理 2-已受理 3-已转出 4-已转入 5-已完成） */
    private Integer status;
    /** 累计缴费年限（核定） */
    private BigDecimal payYears;
    /** 结转个人账户余额 */
    private BigDecimal fundBalance;
    /** 转移完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transferTime;
    /** 经办人 */
    private String handler;
    /** 经办意见 */
    private String handleOpinion;
    /** 备注 */
    private String remark;

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
