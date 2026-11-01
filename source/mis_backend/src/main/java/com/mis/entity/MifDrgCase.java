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
 * 病例入组与支付监管（DRG/DIP）
 */
@Data
@TableName("mif_drg_case")
public class MifDrgCase implements Serializable {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 病例号（结算单号） */
    private String caseNo;
    /** 关联结算单ID */
    private Long billId;
    private String billNo;
    private String patientName;
    private String idCardMask;
    private String insuranceType;
    /** 支付方式（1-DRG 2-DIP） */
    private Integer payType;
    /** 入组病组/病种 */
    private String groupCode;
    private String groupName;
    /** 权重 / 分值 */
    private BigDecimal weight;
    /** 预估支付标准（权重×费率 或 分值×分值成本） */
    private BigDecimal estCost;
    /** 实际费用 */
    private BigDecimal actualCost;
    /** 偏差率（实际/预估-1） */
    private BigDecimal deviationRate;
    /** 状态（1-正常 2-偏差预警 3-费用超支 4-特例单议 5-已入组待审核） */
    private Integer status;
    /** 偏差原因说明 */
    private String deviationReason;
    /** 结算日期 */
    private LocalDate caseDate;

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
