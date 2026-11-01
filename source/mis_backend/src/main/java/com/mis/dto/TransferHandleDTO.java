package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferHandleDTO {

    @NotNull(message = "转移单ID不能为空")
    private Long id;
    /** 处置动作（2-受理 3-转出 4-转入 5-完成转移） */
    @NotNull(message = "处置动作不能为空")
    private Integer action;
    /** 累计缴费年限（核定） */
    private BigDecimal payYears;
    /** 经办意见 */
    private String opinion;
}
