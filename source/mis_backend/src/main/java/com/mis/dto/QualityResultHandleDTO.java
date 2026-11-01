package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QualityResultHandleDTO {

    @NotNull(message = "质控结果ID不能为空")
    private Long id;
    /** 处理状态（1-已确认 2-已整改 3-已忽略） */
    @NotNull(message = "处理状态不能为空")
    private Integer handleStatus;
    /** 处理意见 */
    private String handleOpinion;
    /** 是否生成违规追回扣款单 */
    private Boolean createRecover;
}
