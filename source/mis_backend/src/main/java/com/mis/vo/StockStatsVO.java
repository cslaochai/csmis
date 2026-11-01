package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 进销存看板统计
 */
@Data
public class StockStatsVO implements Serializable {

    /** 在册品项数 */
    private Integer itemCount;
    /** 有库存的品项数 */
    private Integer stockedItemCount;
    /** 当前库存总量 */
    private BigDecimal totalQty;
    /** 当前库存总金额 */
    private BigDecimal totalAmount;
    /** 低于预警下限的品项数 */
    private Integer warnCount;
    /** 负库存（账实不符）品项数 */
    private Integer abnormalCount;
    /** 近 30 天入库金额 */
    private BigDecimal inAmount30;
    /** 近 30 天出库金额 */
    private BigDecimal outAmount30;
    /** 近 30 天入库笔数 */
    private Integer inCount30;
    /** 近 30 天出库笔数 */
    private Integer outCount30;
    /** 近 30 天入库数量 */
    private BigDecimal inQty30;
    /** 近 30 天出库数量 */
    private BigDecimal outQty30;
    /** 盘点差异笔数 */
    private Integer stocktakeDiffCount;
    /** 账实相符率（%，按盘点笔次） */
    private BigDecimal matchRate;
}
