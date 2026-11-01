package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountTxnVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String txnNo;
    private String idCard;
    private String personName;
    private Integer txnType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime txnTime;
    private String reason;
    private String refNo;
    private Integer status;
    private String remark;
}
