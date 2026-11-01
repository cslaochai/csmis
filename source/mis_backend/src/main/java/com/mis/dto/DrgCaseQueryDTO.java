package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DrgCaseQueryDTO extends BizPageParam {

    private String keyword;
    /** 支付方式（1-DRG 2-DIP） */
    private Integer payType;
    /** 状态（1-正常 2-偏差预警 3-费用超支 4-特例单议） */
    private Integer status;
    /** 是否仅看超支 */
    private Boolean overBudget;
}
