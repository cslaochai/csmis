package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FraudClueHandleDTO {

    @NotNull(message = "线索ID不能为空")
    private Long id;
    /** 目标状态（2-已确认 3-已排除 4-已移送） */
    @NotNull(message = "处置状态不能为空")
    private Integer status;
    private String handleUser;
    private String handleOpinion;
}
