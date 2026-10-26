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
 * 平台侧结算单出参：机构 2304 报盘接收台账列表行与详情（详情含原始报文）。
 */
@Data
public class SettlementVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String tradeNo;
    private String settlementNo;
    private String origTradeNo;
    private String fixMedinsCode;
    private String billNo;
    private String encounterType;
    private String patientName;
    private String idCard;
    private String insuranceNo;
    private String insuranceType;
    private BigDecimal totalAmount;
    private BigDecimal insurancePay;
    private BigDecimal personalPay;
    private BigDecimal selfPay;
    /** 平台核定统筹 */
    private BigDecimal poolConfirm;
    /** 状态（1-已接收有效 2-已撤销冲正） */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate billDate;
    private String receiptNo;
    private String warnMsg;
    private String rawPayload;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;
}
