package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelfCheckActionDTO {

    @NotNull(message = "任务ID不能为空")
    private Long id;
    /** 上报/整改内容 */
    private String content;
    /** 复核意见（复核不通过时必填） */
    private String opinion;
    /** 复核结论（true-通过办结 false-退回整改） */
    private Boolean passed;
}
