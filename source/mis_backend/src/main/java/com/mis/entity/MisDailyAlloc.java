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
 * 日拨付（按笔）批次 —— 即时结算改革核心：T+1 按笔拨付医保基金，银行回单回传后次月扣减
 */
@Data
@TableName("mis_daily_alloc")
public class MisDailyAlloc implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 业务日期（结算发生的账期日） */
    private String bizDate;
    /** 日拨付批次号（YBD + yyyyMMdd） */
    private String batchNo;
    /** 当日结算笔数 */
    private Integer settleCount;
    /** 当日医保基金支付总额（申报统筹） */
    private BigDecimal totalFund;
    /** 本次按笔拨付金额（totalFund * 日拨付比例） */
    private BigDecimal payAmount;
    /** 回单状态（0-待拨付 1-已回单） */
    private Integer remitStatus;
    /** 回单编号（银行电子回单） */
    private String receiptNo;
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
