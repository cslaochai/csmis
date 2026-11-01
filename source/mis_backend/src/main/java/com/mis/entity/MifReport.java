package com.mis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 举报投诉与奖励
 */
@Data
@TableName("mif_report")
public class MifReport implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 举报编号（唯一） */
    private String reportNo;
    /** 举报人 */
    private String reporterName;
    private String reporterContact;
    /** 被举报机构/主体 */
    private String reportedMedinsCode;
    private String reportedMedinsName;
    /** 被举报对象（人员） */
    private String reportedSubject;
    /** 问题类型（违规收费/欺诈骗保/服务态度/其他） */
    private String problemType;
    /** 举报内容 */
    private String content;
    /** 附件URL */
    private String attachUrl;
    /** 状态（1-待受理 2-核查中 3-已办结 4-已奖励发放） */
    private Integer status;
    private String handleUser;
    private String handleOpinion;
    /** 奖励金额 */
    private BigDecimal rewardAmount;
    /** 核查时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

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
