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
 * 飞检计划
 */
@Data
@TableName("mif_inspection_plan")
public class MifInspectionPlan implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 计划编号 */
    private String planNo;

    /** 计划名称 */
    private String planName;

    /** 检查类型（1-日常 2-专项 3-监督抽查） */
    private Integer checkType;

    /** 计划周期 */
    private String planPeriod;

    /** 计划检查机构数 */
    private Integer targetCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 开始日期 */
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 结束日期 */
    private LocalDate endDate;

    /** 牵头部门 */
    private String deptName;

    /** 状态（1-已发布 2-执行中 3-已完成） */
    private Integer status;

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