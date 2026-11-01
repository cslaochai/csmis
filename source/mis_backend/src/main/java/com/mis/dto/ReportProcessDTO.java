package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportProcessDTO {

    @NotNull(message = "举报ID不能为空")
    private Long id;
    /** 目标状态（2-核查中 3-已办结） */
    @NotNull(message = "处置状态不能为空")
    private Integer status;
    private String handleUser;
    private String handleOpinion;
}
