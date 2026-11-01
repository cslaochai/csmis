package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TraceCodeVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String code;
    private String drugName;
    private String spec;
    private String manufacturer;
    private String batchNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;
    private Integer status;
    private Long billId;
    private String billNo;
    private Long itemId;
    private String itemName;
    private String scanUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scanTime;
    private Integer verifyCount;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
