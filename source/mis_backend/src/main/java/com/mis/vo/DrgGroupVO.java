package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DrgGroupVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String groupCode;
    private String groupName;
    private Integer payType;
    private BigDecimal weight;
    private BigDecimal rate;
    private BigDecimal pointValue;
    private BigDecimal pointCost;
    private BigDecimal avgCost;
    private Integer caseCount;
    private Integer status;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
