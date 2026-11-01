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
 * 退休人员认定
 */
@Data
@TableName("mis_insure_retire")
public class MisInsureRetire implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 身份证号 */
    private String idCard;

    /** 姓名 */
    private String personName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 退休日期 */
    private LocalDate retireDate;

    /** 类型（1-职工退休 2-居民养老 3-特殊工种） */
    private String retireType;

    /** 累计缴费年限 */
    private BigDecimal payYears;

    /** 状态（0-待审核 1-已认定 2-不通过） */
    private Integer status;

    /** 审核人 */
    private String reviewer;

    /** 审核意见 */
    private String reviewOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 审核时间 */
    private LocalDateTime reviewTime;

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