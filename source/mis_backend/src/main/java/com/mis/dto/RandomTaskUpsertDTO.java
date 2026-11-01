package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RandomTaskUpsertDTO {

    private Long id;
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    /** 检查类型（1-日常检查 2-专项检查 3-监督抽查） */
    @NotNull(message = "检查类型不能为空")
    private Integer checkType = 1;
    /** 计划抽取机构数 */
    private Integer planCount = 5;
    /** 计划抽取检查人员数 */
    private Integer planCheckerCount = 2;
    private String deptName;
    private String remark;
}
