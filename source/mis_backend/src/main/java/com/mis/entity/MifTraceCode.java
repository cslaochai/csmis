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
 * 药品追溯码台账（唯一码采集与核销）
 */
@Data
@TableName("mif_trace_code")
public class MifTraceCode implements Serializable {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 追溯码（唯一） */
    private String code;
    /** 药品名称 */
    private String drugName;
    /** 规格 */
    private String spec;
    /** 生产企业 */
    private String manufacturer;
    /** 批次号 */
    private String batchNo;
    /** 有效期 */
    private LocalDate expireDate;
    /** 状态（1-未使用 2-已使用 3-已作废 4-已退回） */
    private Integer status;
    /** 关联结算单ID */
    private Long billId;
    /** 关联结算单号 */
    private String billNo;
    /** 关联费用明细ID */
    private Long itemId;
    /** 关联项目名称 */
    private String itemName;
    /** 扫码人 */
    private String scanUser;
    /** 扫码核销时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scanTime;
    /** 核验次数 */
    private Integer verifyCount;
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
