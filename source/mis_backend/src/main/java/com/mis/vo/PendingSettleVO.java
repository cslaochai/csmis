package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 待结算业务出参（含费用明细；独立 VO，不继承 entity）。
 * bizNo 为业务号，门诊即就诊号 visitNo、住院即住院号 admissionNo，按 bizType 派生输出。
 */
@Data
public class PendingSettleVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 业务类型（1-门诊 2-住院） */
    private Integer bizType;
    /** 业务号（门诊就诊号/住院号） */
    private String bizNo;
    private String patientName;
    private String idCard;
    private String idCardMask;
    private String insuranceType;
    private String dept;
    private String doctor;
    private Integer admitDays;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bizTime;
    /** 状态（1-待结算 2-已结算） */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 费用明细（ownerType=1） */
    private List<FeeItemVO> items;

    public String getVisitNo() {
        return getBizType() != null && getBizType() == 1 ? getBizNo() : null;
    }

    public String getAdmissionNo() {
        return getBizType() != null && getBizType() == 2 ? getBizNo() : null;
    }

    /** 避免 bizNo 与派生号并存造成混淆，不直接输出 bizNo（业务号由 visitNo/admissionNo 表达） */
    @JsonIgnore
    public String getBizNo() {
        return this.bizNo;
    }
}
