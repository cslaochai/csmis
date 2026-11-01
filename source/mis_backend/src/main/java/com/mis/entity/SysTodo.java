package com.mis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * 待办任务
 */
@Data
@TableName("sys_todo")
public class SysTodo implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 待办类型（如 mis_bill / mif_suspicion） */
    private String todoType;

    /** 待办标题 */
    private String todoTitle;

    /** 业务类型 */
    private String bizType;

    /** 业务主键ID */
    private Long bizId;

    /** 业务编号 */
    private String bizNo;

    /** 处理人 */
    private String assignee;

    /** 处理人ID */
    private Long assigneeId;

    /** 处理部门 */
    private String assignDept;

    /** 优先级（1-高 2-中 3-低） */
    private Integer priority;

    /** 状态（0-待处理 1-处理中 2-已完成 3-已取消） */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 处理意见 */
    private String finishOpinion;

    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createById;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateById;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}