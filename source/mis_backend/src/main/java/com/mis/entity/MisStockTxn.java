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
 * 药品耗材进销存流水
 * <p>方向：入库 qty/amount 为正，出库为负；beforeQty/afterQty 记录变动前后库存，便于账实核对。
 */
@Data
@TableName("mis_stock_txn")
public class MisStockTxn implements Serializable {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 流水号（唯一） */
    private String txnNo;
    /** 库存主档ID */
    private Long itemId;
    /** 品项编码 */
    private String itemCode;
    /** 品项名称 */
    private String itemName;
    /** 交易类型（1-采购入库 2-销售出库 3-退药入库 4-报损出库 5-盘点调整） */
    private Integer txnType;
    /** 数量（入库为正，出库为负） */
    private BigDecimal qty;
    /** 单价 */
    private BigDecimal price;
    /** 金额（入库为正，出库为负） */
    private BigDecimal amount;
    /** 变动前库存 */
    private BigDecimal beforeQty;
    /** 变动后库存 */
    private BigDecimal afterQty;
    /** 批次号 */
    private String batchNo;
    /** 有效期 */
    private LocalDate expireDate;
    /** 关联单号（结算单号 / 盘点单号） */
    private String refNo;
    /** 业务日期 */
    private LocalDate bizDate;
    /** 操作人 */
    private String operator;
    /** 异常提示（如库存不足导致负库存） */
    private String warnMsg;
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
