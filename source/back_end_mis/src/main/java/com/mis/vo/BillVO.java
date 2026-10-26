package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结算单出参（含费用明细；独立 VO，不继承 entity）
 */
@Data
public class BillVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String billNo;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pendingId;
    private String bizNo;
    private Integer admitDays;
    private String patientName;
    private String idCard;
    private String idCardMask;
    private String insuranceType;
    /** 结算类型（门诊/住院） */
    private String settleType;
    private String dept;
    private String doctor;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime settleTime;
    /** 状态（已结算/待审核/已退费/异常） */
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal insurancePay;
    private BigDecimal personalAccount;
    private BigDecimal cashPay;
    private BigDecimal selfPay;
    private BigDecimal deductible;
    private BigDecimal basePool;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 费用明细（ownerType=2） */
    private List<FeeItemVO> items;
}
