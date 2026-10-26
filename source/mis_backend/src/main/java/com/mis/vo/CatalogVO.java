package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 医保三大目录出参：目录分页列表行。
 */
@Data
public class CatalogVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String ybCode;
    private String name;
    private String category;
    private String spec;
    private String unit;
    /** 支付类别（甲/乙/丙） */
    private String payClass;
    /** 乙类先行自付比例（0-1） */
    private BigDecimal selfRatio;
    /** 政策报销比例（0-1） */
    private BigDecimal insuranceRatio;
    private BigDecimal limitPrice;
    /** 状态（1-启用 0-停用） */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
