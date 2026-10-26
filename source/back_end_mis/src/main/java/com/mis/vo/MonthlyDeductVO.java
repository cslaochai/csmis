package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度扣款/拒付明细出参（MonthlyResultVO.deducts）
 */
@Data
public class MonthlyDeductVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String month;
    private String tradeNo;
    private String settlementNo;
    /** 扣款类型（1-拒付 2-审核扣款） */
    private Integer deductType;
    private BigDecimal deductAmount;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
