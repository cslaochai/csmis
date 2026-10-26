package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 网关实时结算返回（2201 预结算 / 2301 正式结算共用）。
 *
 * <p>infcode=0 成功，-1 失败；2301 成功时回 receiptNo（医保结算单号）与 settlementNo。</p>
 */
@Data
public class GatewaySettleVO implements Serializable {

    /** 业务码：0 成功 / -1 失败 */
    private Integer infcode;
    private String tradeNo;
    /** 医保结算单号（YBZ+yyyyMMdd+序列），2301 成功时回 */
    private String receiptNo;
    /** 医保结算清单号 */
    private String settlementNo;

    /** 险种：职工医保 / 居民医保 */
    private String insuranceType;
    /** 参保人姓名（实名核验用） */
    private String name;

    /** 费用总额 */
    private BigDecimal total;
    /** 统筹支付（医保基金付） */
    private BigDecimal insurancePay;
    /** 个账支付（从参保人个人账户扣） */
    private BigDecimal personalAccount;
    /** 现金支付（患者当场掏） */
    private BigDecimal cashPay;
    /** 个人自付总额（= 个账 + 现金） */
    private BigDecimal selfPay;
    /** 起付线（本次实际计入） */
    private BigDecimal deductible;

    /** 个账扣后余额（2301 成功时回；2201 回当前余额） */
    private BigDecimal accountBalanceAfter;

    private String warnMsg;
    private String errMsg;
}
