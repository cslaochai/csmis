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
 * 行政处罚
 */
@Data
@TableName("mif_penalty")
public class MifPenalty implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 处罚决定书号 */
    private String penaltyNo;

    /** 被处罚机构编码 */
    private String medinsCode;

    /** 被处罚机构名称 */
    private String medinsName;

    /** 处罚种类（1-警告 2-罚款 3-暂停 4-解除） */
    private String penaltyType;

    /** 违法事实与理由 */
    private String penaltyReason;

    /** 罚款金额 */
    private BigDecimal penaltyAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 决定日期 */
    private LocalDate penaltyDate;

    /** 决定书文号 */
    private String decisionDoc;

    /** 状态（0-拟稿 1-已决定 2-已送达 3-已执行） */
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