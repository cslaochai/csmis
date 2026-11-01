package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TraceLogQueryDTO extends BizPageParam {

    private String keyword;
    /** 核验结果（1-核验通过 2-追溯码不存在 3-重复使用 4-与结算项目不符 5-已过期 6-已作废） */
    private Integer verifyResult;
    private String billNo;
}
