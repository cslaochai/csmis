package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SelfCheckUpsertDTO {

    private Long id;
    @NotBlank(message = "定点机构名称不能为空")
    private String medinsName;
    private String medinsCode;
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    /** 自查范围 */
    private String checkScope;
    /** 整改截止日期 */
    private LocalDate deadline;
    private String remark;
}
