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
 * 生育待遇申报
 */
@Data
@TableName("mis_maternity_claim")
public class MisMaternityClaim implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 申报单号 */
    private String claimNo;

    /** 身份证号 */
    private String idCard;

    /** 姓名 */
    private String personName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 分娩日期 */
    private LocalDate deliveryDate;

    /** 类型（1-顺产 2-剖宫产 3-难产） */
    private Integer deliveryType;

    /** 新生儿数量 */
    private Integer babyCount;

    /** 申报金额 */
    private BigDecimal applyAmount;

    /** 核准支付金额 */
    private BigDecimal payAmount;

    /** 状态（0-待审核 1-已核准 2-已支付 3-已驳回） */
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