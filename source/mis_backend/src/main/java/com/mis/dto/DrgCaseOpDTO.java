package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DrgCaseOpDTO {

    @NotNull(message = "病例ID不能为空")
    private Long id;
    /** 处置动作（4-特例单议 1-恢复正常） */
    private Integer action;
    /** 处置说明 */
    private String opinion;
}
