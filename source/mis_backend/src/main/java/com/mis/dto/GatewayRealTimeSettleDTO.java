package com.mis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 网关实时结算报文（2201 预结算 / 2301 正式结算共用入参）。
 *
 * <p>与 2304 事后报盘的区别：2304 是 HIS 把已落账的清单事后推过来留痕；
 * 2201/2301 是收费员在窗口点「医保个账」时<b>实时</b>调，平台当场算 splits、
 * 2301 当场扣个账并出医保结算单号。</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayRealTimeSettleDTO {

    /** HIS 侧流水号（幂等键，2301 同一 tradeNo 重复提交回原结果） */
    private String tradeNo;
    /** HIS 侧账单号（对账用） */
    private String billNo;

    /** 就诊类型（1-门诊 2-住院） */
    private String encounterType;

    private String idCard;
    private String insuranceNo;
    private String patientName;

    private List<Item> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        /** 医保目录编码 */
        private String ybCode;
        /** 项目名称 */
        private String name;
        /** 目录支付类别：甲 / 乙 / 丙（丙=全自费） */
        private String payClass;
        /** 乙类先行自付比例（0~1），甲/丙传 null 或 0 */
        private BigDecimal selfRatio;
        private BigDecimal price;
        private Integer qty;
    }
}
