package com.mis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 反欺诈线索（模型命中后生成，进入核查/处置闭环）
 */
@Data
@TableName("mif_fraud_clue")
public class MifFraudClue implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 线索编号（唯一） */
    private String clueNo;
    /** 来源模型 */
    private Long modelId;
    private String modelCode;
    private String modelName;
    /** 涉及机构 */
    private String medinsCode;
    private String medinsName;
    /** 涉及参保人 */
    private String insuredIdCard;
    private String insuredName;
    /** 关联业务单号 */
    private String bizNo;
    /** 涉及金额 */
    private BigDecimal involveAmount;
    /** 风险等级（1-低 2-中 3-高） */
    private Integer riskLevel;
    /** 线索详情（JSON / 文本） */
    private String clueDetail;
    /** 状态（1-待核查 2-已确认 3-已排除 4-已移送） */
    private Integer status;
    /** 核查人 */
    private String handleUser;
    /** 核查时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    /** 核查/处置意见 */
    private String handleOpinion;

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
