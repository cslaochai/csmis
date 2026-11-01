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
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * 结算清单质控结果（命中留痕）
 */
@Data
@TableName("mif_quality_result")
public class MifQualityResult implements Serializable {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 关联结算单ID */
    private Long billId;
    /** 结算单号 */
    private String billNo;
    /** 关联费用明细ID */
    private Long itemId;
    /** 项目名称 */
    private String itemName;
    /** 医保编码 */
    private String ybCode;
    /** 定点机构编码 */
    private String medinsCode;
    /** 定点机构名称 */
    private String medinsName;
    /** 规则ID */
    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    /** 校验类型 */
    private String ruleType;
    /** 严重程度（1-提示 2-警示 3-严重） */
    private Integer severity;
    /** 质控字段 */
    private String fieldName;
    /** 原始值 */
    private String originValue;
    /** 质控说明 */
    private String qualityDesc;
    /** 涉及金额 */
    private BigDecimal involveAmount;
    /** 建议扣款金额 */
    private BigDecimal deductAmount;
    /** 处理状态（0-待处理 1-已确认 2-已整改 3-已忽略） */
    private Integer handleStatus;
    /** 处理意见 */
    private String handleOpinion;
    /** 处理人 */
    private String handleUser;
    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    /** 质控时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkTime;

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
