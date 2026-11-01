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
 * 结算清单（医疗机构上传的清单主表）
 */
@Data
@TableName("mis_settle_list")
public class MisSettleList implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 清单编号 */
    private String listNo;

    /** 关联结算单ID */
    private Long billId;

    /** 结算单号 */
    private String billNo;

    /** 医疗机构编码 */
    private String medinsCode;

    /** 医疗机构名称 */
    private String medinsName;

    /** 患者姓名 */
    private String patientName;

    /** 身份证掩码 */
    private String idCardMask;

    /** 险种 */
    private String insuranceType;

    /** 就诊类型 */
    private String encounterType;

    /** 清单项目数 */
    private Integer itemCount;

    /** 费用总额 */
    private BigDecimal totalAmount;

    /** 统筹支付 */
    private BigDecimal insurancePay;

    /** 清单版本 */
    private String listVersion;

    /** 上传批次号 */
    private String uploadBatchNo;

    /** 质控状态（0-未质控 1-通过 2-有问题） */
    private Integer checkStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 上传时间 */
    private LocalDateTime uploadTime;

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