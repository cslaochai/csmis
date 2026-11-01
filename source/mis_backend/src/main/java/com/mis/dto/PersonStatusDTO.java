package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 参保状态变更入参（停保 / 恢复）
 */
@Data
public class PersonStatusDTO {

    @NotNull(message = "参保人ID不能为空")
    private Long id;

    /** 目标状态（1-正常参保 0-停保） */
    @NotNull(message = "目标参保状态不能为空")
    private Integer personStatus;

    /** 停保/恢复原因（可选，留痕） */
    private String remark;
}
