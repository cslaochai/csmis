package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 盘点明细录入
 */
@Data
public class StockCheckItemDTO implements Serializable {

    /** 盘点单号 */
    @NotBlank(message = "盘点单号不能为空")
    private String checkNo;

    /** 品项编码 */
    @NotBlank(message = "品项编码不能为空")
    private String itemCode;

    /** 实盘数量 */
    private BigDecimal realQty;

    /** 差异原因 */
    private String diffReason;
}
