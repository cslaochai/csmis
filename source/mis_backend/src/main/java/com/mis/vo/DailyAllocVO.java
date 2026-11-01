package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 日拨付批次出参
 */
@Data
public class DailyAllocVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String bizDate;
    private String batchNo;
    private Integer settleCount;
    private BigDecimal totalFund;
    private BigDecimal payAmount;
    private Integer remitStatus;
    private String receiptNo;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
