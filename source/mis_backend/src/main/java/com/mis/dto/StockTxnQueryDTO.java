package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockTxnQueryDTO extends BizPageParam {

    private String keyword;
    /** 品项ID */
    private Long itemId;
    /** 交易类型（1-采购入库 2-销售出库 3-退药入库 4-报损出库 5-盘点调整） */
    private Integer txnType;
    /** 关联单号 */
    private String refNo;
}
