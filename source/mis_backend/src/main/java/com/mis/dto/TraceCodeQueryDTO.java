package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TraceCodeQueryDTO extends BizPageParam {

    private String keyword;
    /** 药品名称 */
    private String drugName;
    /** 状态（1-未使用 2-已使用 3-已作废 4-已退回） */
    private Integer status;
    /** 关联结算单号 */
    private String billNo;
}
