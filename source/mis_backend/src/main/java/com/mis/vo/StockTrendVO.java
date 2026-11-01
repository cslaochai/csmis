package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 进销存日趋势
 */
@Data
public class StockTrendVO implements Serializable {

    /** 日期 */
    private String day;
    /** 入库金额 */
    private BigDecimal inAmount;
    /** 出库金额 */
    private BigDecimal outAmount;
    /** 入库数量 */
    private BigDecimal inQty;
    /** 出库数量 */
    private BigDecimal outQty;
}
