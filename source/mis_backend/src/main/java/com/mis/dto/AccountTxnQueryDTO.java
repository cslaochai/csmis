package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AccountTxnQueryDTO extends BizPageParam {

    private String keyword;
    private String idCard;
    /** 交易类型（1-划入 2-结算划扣 3-一次性支取 4-转移出 5-转移入 6-清退 7-退保冲回） */
    private Integer txnType;
}
