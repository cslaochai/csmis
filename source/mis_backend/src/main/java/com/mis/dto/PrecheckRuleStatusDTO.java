package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrecheckRuleStatusDTO {

    @NotNull(message = "规则ID不能为空")
    private Long id;
    @NotNull(message = "状态不能为空")
    private Integer status;
}
