package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FraudModelUpsertDTO {

    private Long id;
    @NotBlank(message = "模型编码不能为空")
    private String modelCode;
    @NotBlank(message = "模型名称不能为空")
    private String modelName;
    /** 欺诈类别 */
    private String category;
    /** 风险等级（1-低 2-中 3-高） */
    private Integer riskLevel = 2;
    /** 算法/规则描述 */
    private String algorithm;
    /** 模型参数（JSON） */
    private String params;
    /** 状态（1-启用 0-停用） */
    private Integer status = 1;
    /** 说明 */
    private String description;
}
