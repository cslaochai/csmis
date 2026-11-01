package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TraceLogVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String code;
    private Long billId;
    private String billNo;
    private String itemName;
    private String drugName;
    private Integer verifyResult;
    private String problemDesc;
    private String verifier;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifyTime;
}
