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
 * 结算批次
 */
@Data
@TableName("mis_settle_batch")
public class MisSettleBatch implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 批次号 */
    private String batchNo;

    /** 类型（1-实时结算 2-日批 3-月度） */
    private Integer batchType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 业务日期 */
    private LocalDate bizDate;

    /** 账期（yyyy-MM） */
    private String month;

    /** 清单数 */
    private Integer listCount;

    /** 费用总额 */
    private BigDecimal totalAmount;

    /** 统筹支付 */
    private BigDecimal insurancePay;

    /** 状态（0-上传中 1-已接收 2-已清算 3-异常） */
    private Integer status;

    /** 上传人 */
    private String uploadUser;

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