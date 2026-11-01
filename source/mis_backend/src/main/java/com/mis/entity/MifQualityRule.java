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
 * 结算清单质控规则
 */
@Data
@TableName("mif_quality_rule")
public class MifQualityRule implements Serializable {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 规则编码（唯一） */
    private String ruleCode;
    /** 规则名称 */
    private String ruleName;
    /** 校验类型（required_field-必填项缺失 not_mapped-目录未对照 over_limit_price-超限价 invalid_qty-数量异常 pay_class_missing-支付类别缺失 duplicate_report-重复上报 amount_mismatch-金额勾稽不一致 catalog_disabled-目录已停用 self_ratio_missing-自付比例缺失 gender_limit-性别限制） */
    private String ruleType;
    /** 严重程度（1-提示 2-警示 3-严重） */
    private Integer severity;
    /** 扣款比例（0-1，按该条金额扣减；0-仅提示） */
    private BigDecimal deductRatio;
    /** 扣款方式（1-按比例扣减 2-按固定金额扣减） */
    private Integer deductType;
    /** 固定扣款金额（扣款方式=2 时生效） */
    private BigDecimal deductAmount;
    /** 规则参数（JSON） */
    private String params;
    /** 状态（1-启用 0-停用） */
    private Integer status;
    /** 说明 */
    private String description;

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
