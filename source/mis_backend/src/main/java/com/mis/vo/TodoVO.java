package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 待办任务返回对象
 */
@Data
public class TodoVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 待办类型 */
    private String todoType;

    /** 待办标题 */
    private String todoTitle;

    /** 业务类型 */
    private String bizType;

    /** 业务主键ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizId;

    /** 业务编号 */
    private String bizNo;

    /** 处理人 */
    private String assignee;

    /** 处理人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assigneeId;

    /** 处理部门 */
    private String assignDept;

    /** 优先级（1-高 2-中 3-低） */
    private Integer priority;

    /** 状态（0-待处理 1-处理中 2-已完成 3-已取消） */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /** 处理意见 */
    private String finishOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
