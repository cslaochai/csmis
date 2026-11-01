package com.mis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 事前提醒 / 事中审核拦截规则（三道防线之前两道）
 */
@Data
@TableName("mif_precheck_rule")
public class MifPrecheckRule implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 规则编码（唯一） */
    private String ruleCode;
    /** 规则名称 */
    private String ruleName;
    /** 触发阶段（before_settle-事前提醒 in_settle-事中拦截） */
    private String triggerStage;
    /** 风险等级（1-低 2-中 3-高） */
    private Integer riskLevel;
    /** 提醒/拦截提示语 */
    private String promptText;
    /** 是否阻断（1-阻断结算 0-仅提醒） */
    private Integer blockFlag;
    /** 类别（冒名/超量开药/超限价/重复收费等） */
    private String category;
    /** 规则参数（JSON，如 {"threshold":5,"windowDays":30}） */
    private String params;
    /** 状态（1-启用 0-停用） */
    private Integer status;
    /** 说明 */
    private String description;

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
