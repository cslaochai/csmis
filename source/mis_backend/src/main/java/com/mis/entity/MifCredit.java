package com.mis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医保信用管理：定点机构 / 人员信用评价、"驾照式"记分、守信激励 / 失信惩戒名单
 */
@Data
@TableName("mif_credit")
public class MifCredit implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 主体类型（1-定点机构 2-医药从业人员） */
    private Integer subjectType;
    /** 主体编码（机构编码 / 人员工号） */
    private String subjectCode;
    /** 主体名称 */
    private String subjectName;
    /** 信用等级（A-优秀 B-良好 C-一般 D-差） */
    private String creditLevel;
    /** 信用评分 */
    private Integer score;
    /** 违规记分次数 */
    private Integer violationCount;
    /** 奖惩类型（0-无 1-守信激励 2-失信惩戒） */
    private Integer punishType;
    private String remark;

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
