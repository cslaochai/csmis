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
 * 信用记分明细
 */
@Data
@TableName("mif_credit_detail")
public class MifCreditDetail implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 主体类型（1-机构 2-医师 3-参保人） */
    private Integer subjectType;

    /** 主体编码 */
    private String subjectCode;

    /** 主体名称 */
    private String subjectName;

    /** 事件类型（违规扣分/守信加分/整改恢复） */
    private String eventType;

    /** 分值变动（扣分为负） */
    private BigDecimal scoreChange;

    /** 变动后总分 */
    private BigDecimal scoreAfter;

    /** 来源类型 */
    private String sourceType;

    /** 来源单号 */
    private String sourceNo;

    /** 事由 */
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 发生时间 */
    private LocalDateTime occurTime;

    /** 操作人 */
    private String operator;

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