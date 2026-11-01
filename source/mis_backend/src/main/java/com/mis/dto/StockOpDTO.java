package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出入库 / 盘点操作入参
 */
@Data
public class StockOpDTO {

    /** 品项编码 */
    @NotBlank(message = "品项编码不能为空")
    private String itemCode;
    /** 操作类型（1-采购入库 2-销售出库 3-退药入库 4-报损出库 5-盘点调整） */
    @NotNull(message = "操作类型不能为空")
    private Integer opType;
    /** 数量（入库正数、出库正数、盘点为实盘数） */
    @NotNull(message = "数量不能为空")
    private BigDecimal qty;
    /** 入库单价（出库忽略） */
    private BigDecimal price;
    /** 盘点时的实盘金额（可选，用于修正均价） */
    private BigDecimal realAmount;
    private String batchNo;
    private LocalDate expireDate;
    /** 关联单号 */
    private String refNo;
    private LocalDate bizDate;
    private String remark;
}
