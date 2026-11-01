package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrecheckResultHandleDTO {

    @NotNull(message = "结果ID不能为空")
    private Long id;
    private String handleOpinion;
}
