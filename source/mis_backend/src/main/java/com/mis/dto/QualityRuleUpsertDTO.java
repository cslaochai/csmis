package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QualityRuleUpsertDTO {

    private Long id;
    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;
    /** 校验类型 */
    @NotBlank(message = "校验类型不能为空")
    private String ruleType;
    /** 严重程度（1-提示 2-警示 3-严重） */
    @NotNull(message = "严重程度不能为空")
    private Integer severity = 2;
    /** 扣款比例（0-1） */
    private BigDecimal deductRatio = BigDecimal.ZERO;
    /** 扣款方式（1-按比例 2-固定金额） */
    private Integer deductType = 1;
    private BigDecimal deductAmount = BigDecimal.ZERO;
    /** 规则参数（JSON） */
    private String params;
    private Integer status = 1;
    private String description;
}
