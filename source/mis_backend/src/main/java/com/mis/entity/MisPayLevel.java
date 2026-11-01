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
 * 缴费档次
 */
@Data
@TableName("mis_pay_level")
public class MisPayLevel implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 档次编码 */
    private String levelCode;

    /** 档次名称 */
    private String levelName;

    /** 适用险种 */
    private String insuranceType;

    /** 缴费基数 */
    private BigDecimal payBase;

    /** 缴费金额 */
    private BigDecimal payAmount;

    /** 个人账户划入比例 */
    private BigDecimal accountRatio;

    /** 对应待遇档级 */
    private String benefitLevel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 生效起 */
    private LocalDate effectiveStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 生效止 */
    private LocalDate effectiveEnd;

    /** 状态（1-启用 0-停用） */
    private Integer status;

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