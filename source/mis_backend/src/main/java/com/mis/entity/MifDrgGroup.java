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
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * 支付方式病组/病种池（DRG 分组 与 DIP 病种分值池）
 */
@Data
@TableName("mif_drg_group")
public class MifDrgGroup implements Serializable {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 病组/病种编码（唯一） */
    private String groupCode;
    /** 病组/病种名称 */
    private String groupName;
    /** 支付方式（1-DRG 按病组付费 2-DIP 按病种分值付费） */
    private Integer payType;
    /** DRG 权重 */
    private BigDecimal weight;
    /** DRG 费率（元/权重） */
    private BigDecimal rate;
    /** DIP 分值 */
    private BigDecimal pointValue;
    /** DIP 每分值成本（元/分） */
    private BigDecimal pointCost;
    /** 组均次费用（入组匹配依据） */
    private BigDecimal avgCost;
    /** 病例数 */
    private Integer caseCount;
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
