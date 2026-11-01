package com.mis.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 追溯码批量登记（扫码采集入库）
 * <p>支持两种形态：单条（code + drugName 等）与批量（items 列表）。
 * 校验在 Service 层完成，避免单条/批量两套注解互相干扰。
 */
@Data
public class TraceCodeUpsertDTO {

    /** 单条登记 */
    private String code;
    private String drugName;
    private String spec;
    private String manufacturer;
    private String batchNo;
    private LocalDate expireDate;

    /** 批量登记 */
    private List<TraceCodeUpsertDTO> items;
}
