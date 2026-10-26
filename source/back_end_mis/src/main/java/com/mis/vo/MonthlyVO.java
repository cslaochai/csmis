package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度结算单出参：月度结算管理分页列表行。
 */
@Data
public class MonthlyVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** yyyy-MM */
    private String month;
    private String fixMedinsCode;
    private Integer declareCount;
    private BigDecimal declareAmount;
    private BigDecimal deductAmount;
    private BigDecimal reserveAmount;
    private BigDecimal payableAmount;
    private BigDecimal allocatedAmount;
    /** 状态（1-已申报 2-已审核 3-已拨付 4-已清算） */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime declareTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime allocateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime settleTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
