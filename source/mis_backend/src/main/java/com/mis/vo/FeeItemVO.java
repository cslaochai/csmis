package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用明细出参（待结算 / 结算单共用明细行）
 */
@Data
public class FeeItemVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 归属类型（1-待结算 2-结算单） */
    private Integer ownerType;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerId;
    private String name;
    private String category;
    private String spec;
    private String unit;
    private String ybCode;
    private String payClass;
    private BigDecimal selfRatio;
    private BigDecimal insuranceRatio;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
    /** 是否已冲正 */
    private Boolean reversed;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
