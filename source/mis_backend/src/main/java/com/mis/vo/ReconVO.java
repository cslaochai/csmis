package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 清算对账出参：月度账期对账列表行。
 */
@Data
public class ReconVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 清算账期（yyyy-MM） */
    private String period;
    private Integer billCount;
    private BigDecimal settleAmount;
    private BigDecimal insurancePay;
    private BigDecimal receivedFromFund;
    private BigDecimal diff;
    /** 状态（待对账/已对账/存在差异） */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
