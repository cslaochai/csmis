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
 * 参保缴费记录
 */
@Data
@TableName("mis_insure_pay_record")
public class MisInsurePayRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 参保人ID */
    private Long personId;

    /** 身份证号 */
    private String idCard;

    /** 参保人姓名 */
    private String personName;

    /** 参保单位编码 */
    private String groupCode;

    /** 缴费年度（yyyy） */
    private String payYear;

    /** 缴费月份（yyyy-MM） */
    private String payMonth;

    /** 缴费档次编码 */
    private String payLevelCode;

    /** 缴费基数 */
    private BigDecimal payBase;

    /** 缴费金额 */
    private BigDecimal payAmount;

    /** 划入个人账户金额 */
    private BigDecimal accountAmount;

    /** 类型（1-单位缴 2-个人缴 3-财政补助 4-补缴） */
    private Integer payType;

    /** 状态（1-已到账 2-待到账 3-已退费） */
    private Integer payStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 到账时间 */
    private LocalDateTime payTime;

    /** 来源单号 */
    private String sourceNo;

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