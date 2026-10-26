package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 医保拨付单出参（MonthlyResultVO.allocations：月度拨付 / 质保金返还）
 */
@Data
public class AllocationVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String month;
    private String allocNo;
    /** 拨付类型（1-月度拨付 2-质保金返还/年清算） */
    private Integer allocType;
    private BigDecimal amount;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
