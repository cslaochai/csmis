package com.mis.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 盘点明细返回对象
 */
@Data
public class StockCheckItemVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 盘点单号 */
    private String checkNo;

    /** 品项编码 */
    private String itemCode;

    /** 品项名称 */
    private String itemName;

    /** 账面数量 */
    private BigDecimal bookQty;

    /** 实盘数量 */
    private BigDecimal realQty;

    /** 差异数量（实盘 - 账面） */
    private BigDecimal diffQty;

    /** 差异金额 */
    private BigDecimal diffAmount;

    /** 差异原因 */
    private String diffReason;
}
