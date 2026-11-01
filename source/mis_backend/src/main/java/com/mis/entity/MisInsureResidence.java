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
 * 长期居住备案证明
 */
@Data
@TableName("mis_insure_residence")
public class MisInsureResidence implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 身份证号 */
    private String idCard;

    /** 参保人姓名 */
    private String personName;

    /** 居住地 */
    private String residenceArea;

    /** 类型（1-户籍 2-长期居住 3-工作居住） */
    private String residenceType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 有效期起 */
    private LocalDate validStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 有效期止 */
    private LocalDate validEnd;

    /** 证明编号 */
    private String certNo;

    /** 状态（0-待审核 1-已备案 2-已失效 3-已驳回） */
    private Integer status;

    /** 审核人 */
    private String reviewer;

    /** 审核意见 */
    private String reviewOpinion;

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