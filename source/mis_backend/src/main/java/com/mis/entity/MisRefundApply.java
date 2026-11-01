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
 * 退费申请
 */
@Data
@TableName("mis_refund_apply")
public class MisRefundApply implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 申请单号 */
    private String applyNo;

    /** 结算单ID */
    private Long billId;

    /** 结算单号 */
    private String billNo;

    /** 患者姓名 */
    private String patientName;

    /** 申请退费金额 */
    private BigDecimal refundAmount;

    /** 退费原因 */
    private String refundReason;

    /** 申请人 */
    private String applyUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 状态（0-待审核 1-已通过 2-已驳回 3-已退费） */
    private Integer status;

    /** 审核人 */
    private String reviewer;

    /** 审核意见 */
    private String reviewOpinion;

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