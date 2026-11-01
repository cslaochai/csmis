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
 * 守信激励记录
 */
@Data
@TableName("mif_credit_reward")
public class MifCreditReward implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 主体类型 */
    private Integer subjectType;

    /** 主体编码 */
    private String subjectCode;

    /** 主体名称 */
    private String subjectName;

    /** 激励类型（1-绿色通道 2-减免 3-授牌 4-优先拨付） */
    private String rewardType;

    /** 激励内容 */
    private String rewardDesc;

    /** 减免金额 */
    private BigDecimal rewardAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 生效起 */
    private LocalDate effectiveStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 生效止 */
    private LocalDate effectiveEnd;

    /** 状态 */
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