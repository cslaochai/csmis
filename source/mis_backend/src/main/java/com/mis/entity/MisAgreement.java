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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定点医药机构协议管理（申请 / 签订 / 变更 / 年度考核 / 解除）
 */
@Data
@TableName("mis_agreement")
public class MisAgreement implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 定点机构编码（唯一） */
    private String medinsCode;
    /** 定点机构名称 */
    private String medinsName;
    /** 机构类型（1-定点医院 2-定点药店） */
    private Integer medinsType;
    /** 协议编号 */
    private String agreementNo;
    /** 协议状态（0-申请中 1-已签订 2-已解除） */
    private Integer agreementStatus;
    /** 签订日期 */
    private LocalDate signDate;
    /** 到期日期 */
    private LocalDate expireDate;
    /** 服务范围 */
    private String serviceScope;
    /** 年度考核得分 */
    private Integer annualScore;
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
