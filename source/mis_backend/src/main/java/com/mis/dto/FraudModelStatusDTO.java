package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FraudModelStatusDTO {

    @NotNull(message = "模型ID不能为空")
    private Long id;
    @NotNull(message = "状态不能为空")
    private Integer status;
    private String remark;
}
