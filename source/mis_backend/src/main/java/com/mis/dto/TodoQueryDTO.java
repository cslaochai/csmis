package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 待办查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TodoQueryDTO extends BizPageParam implements Serializable {

    /** 待办类型 */
    private String todoType;

    /** 业务类型 */
    private String bizType;

    /** 处理人 */
    private String assignee;

    /** 优先级 */
    private Integer priority;

    /** 状态（0-待处理 1-处理中 2-已完成 3-已取消） */
    private Integer status;
}
