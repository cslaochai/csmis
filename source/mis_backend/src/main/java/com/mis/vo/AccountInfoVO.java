package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 个人账户查询结果（余额 + 明细）
 */
@Data
public class AccountInfoVO implements Serializable {

    /** 身份证号 */
    private String idCard;
    /** 参保人姓名 */
    private String personName;
    /** 账户余额 */
    private BigDecimal balance;
    /** 累计划入 */
    private BigDecimal totalIn;
    /** 累计支出 */
    private BigDecimal totalOut;
    /** 流水条数 */
    private Integer txnCount;
    /** 明细 */
    private java.util.List<AccountTxnVO> txns;
}
