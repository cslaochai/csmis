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
 * 待遇政策配置
 */
@Data
@TableName("mis_benefit_config")
public class MisBenefitConfig implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 配置编码 */
    private String configCode;

    /** 配置名称 */
    private String configName;

    /** 适用险种 */
    private String insuranceType;

    /** 适用就诊类型（门诊/住院） */
    private String encounterType;

    /** 起付线 */
    private BigDecimal deductible;

    /** 起付线档级（按年度/住院次数） */
    private String deductibleLevel;

    /** 封顶线 */
    private BigDecimal capAmount;

    /** 报销比例 */
    private BigDecimal payRatio;

    /** 个人账户划入/支付比例 */
    private BigDecimal accountRatio;

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