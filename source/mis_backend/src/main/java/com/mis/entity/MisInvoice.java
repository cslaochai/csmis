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
 * 结算凭证
 */
@Data
@TableName("mis_invoice")
public class MisInvoice implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 凭证号 */
    private String invoiceNo;

    /** 账期（yyyy-MM） */
    private String month;

    /** 机构编码 */
    private String medinsCode;

    /** 机构名称 */
    private String medinsName;

    /** 结算笔数 */
    private Integer billCount;

    /** 金额 */
    private BigDecimal amount;

    /** 类型（1-拨付 2-拒付 3-扣款 4-清算） */
    private Integer invoiceType;

    /** 状态（0-待生成 1-已生成 2-已回单） */
    private Integer status;

    /** 回单号 */
    private String receiptNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 生成时间 */

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