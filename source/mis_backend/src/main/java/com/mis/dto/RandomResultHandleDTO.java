package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RandomResultHandleDTO {

    @NotNull(message = "抽取结果ID不能为空")
    private Long id;
    /** 检查结果（1-待检查 2-正常 3-存在问题 4-已整改） */
    @NotNull(message = "检查结果不能为空")
    private Integer result;
    /** 检查情况说明 */
    private String checkResult;
    /** 是否公开（0-未公开 1-已公开） */
    private Integer publicFlag;
}
