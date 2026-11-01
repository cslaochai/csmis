package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportRewardDTO {

    @NotNull(message = "举报ID不能为空")
    private Long id;
    /** 奖励金额 */
    @NotNull(message = "奖励金额不能为空")
    private BigDecimal rewardAmount;
}
