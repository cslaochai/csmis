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
 * 库存盘点单
 */
@Data
@TableName("mif_stock_check")
public class MifStockCheck implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 盘点单号 */
    private String checkNo;

    /** 机构编码 */
    private String medinsCode;

    /** 机构名称 */
    private String medinsName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 盘点日期 */
    private LocalDate checkDate;

    /** 盘点品项数 */
    private Integer itemCount;

    /** 差异品项数 */
    private Integer diffCount;

    /** 差异金额 */
    private BigDecimal diffAmount;

    /** 状态（0-待盘点 1-盘点中 2-已完成 3-已复核） */
    private Integer status;

    /** 盘点人 */
    private String checker;

    /** 复核人 */
    private String reviewer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** 复核时间 */
    private LocalDateTime reviewTime;

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