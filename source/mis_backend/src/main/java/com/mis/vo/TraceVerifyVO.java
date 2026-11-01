package com.mis.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 追溯码核验结果
 */
@Data
public class TraceVerifyVO implements Serializable {

    /** 追溯码 */
    private String code;
    /** 核验结果（1-核验通过 2-追溯码不存在 3-重复使用 4-与结算项目不符 5-已过期 6-已作废） */
    private Integer verifyResult;
    /** 结果说明 */
    private String verifyDesc;
    /** 药品名称 */
    private String drugName;
    /** 关联结算单号 */
    private String billNo;
    /** 是否已核销该码 */
    private Boolean consumed;
}
