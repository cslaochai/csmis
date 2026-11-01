package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 盘点单查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockCheckQueryDTO extends BizPageParam implements Serializable {

    /** 盘点单号 */
    private String checkNo;

    /** 定点机构编码 */
    private String medinsCode;

    /** 状态（1-待盘点 2-盘点中 3-待复核 4-已办结） */
    private Integer status;

    /** 盘点日期（yyyy-MM-dd） */
    private String checkDate;
}
