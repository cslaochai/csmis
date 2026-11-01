package com.mis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
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

/**
 * 手工（零星）报销：参保人门诊 / 住院 / 药店费用报销，受理 → 审核 → 拨付 → 办结
 */
@Data
@TableName("mis_manual_claim")
public class MisManualClaim implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 报销单号 */
    private String claimNo;
    private String personName;
    private String idCard;
    /** 险种（2-城镇职工医保 3-城乡居民医保） */
    private String insuranceType;
    /** 报销类型（1-门诊 2-住院 3-药店） */
    private Integer claimType;
    /** 就诊日期 */
    private LocalDate visitDate;
    /** 费用总额 */
    private BigDecimal totalAmount;
    /** 统筹支付（审核核定） */
    private BigDecimal insurancePay;
    /** 个人自付 */
    private BigDecimal personalPay;
    /** 状态（0-待审核 1-已审核 2-已拨付 3-已退回） */
    private Integer status;
    /** 申请事由 */
    private String applyReason;
    /** 审核意见 */
    private String auditOpinion;
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
