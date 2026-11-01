package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PrecheckRuleUpsertDTO {

    private Long id;
    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;
    /** 触发阶段（before_settle-事前提醒 in_settle-事中拦截） */
    private String triggerStage = "before_settle";
    /** 风险等级（1-低 2-中 3-高） */
    private Integer riskLevel = 2;
    /** 提示语 */
    private String promptText;
    /** 是否阻断（1-阻断 0-提醒） */
    private Integer blockFlag = 0;
    private String category;
    /** 规则参数（JSON） */
    private String params;
    /** 状态（1-启用 0-停用） */
    private Integer status = 1;
    private String description;
}
