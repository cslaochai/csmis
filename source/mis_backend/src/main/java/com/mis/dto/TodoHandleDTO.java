package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 待办处理入参
 */
@Data
public class TodoHandleDTO implements Serializable {

    /** 待办ID */
    @NotNull(message = "待办ID不能为空")
    private Long id;

    /** 处理结果（2-已完成 3-已取消） */
    @NotNull(message = "处理结果不能为空")
    private Integer status;

    /** 处理意见 */
    private String finishOpinion;
}
