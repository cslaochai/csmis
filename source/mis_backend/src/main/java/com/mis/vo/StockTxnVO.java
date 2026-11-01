package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StockTxnVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String txnNo;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private Integer txnType;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal beforeQty;
    private BigDecimal afterQty;
    private String batchNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;
    private String refNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bizDate;
    private String operator;
    private String warnMsg;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
