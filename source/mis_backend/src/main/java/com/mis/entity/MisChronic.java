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
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * 门诊慢特病（门诊特殊病种）待遇认定
 */
@Data
@TableName("mis_chronic")
public class MisChronic implements Serializable {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 认定单号（唯一） */
    private String recordNo;
    private String personName;
    private String idCard;
    private String insuranceNo;
    /** 病种编码 */
    private String diseaseCode;
    /** 病种名称 */
    private String diseaseName;
    /** 待遇类别（1-门诊特殊病种 2-门诊慢特病） */
    private Integer diseaseLevel;
    /** 申请日期 */
    private LocalDate applyDate;
    /** 申请医师 */
    private String doctor;
    /** 定点医疗机构 */
    private String hospital;
    /** 状态（1-待审核 2-已认定 3-不通过 4-已复审） */
    private Integer status;
    /** 待遇有效期起 */
    private LocalDate validFrom;
    /** 待遇有效期止 */
    private LocalDate validTo;
    /** 审核人 */
    private String reviewer;
    /** 审核意见 */
    private String reviewOpinion;
    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
    /** 备注 */
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
