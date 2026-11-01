package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockItemVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String itemCode;
    private String itemName;
    private String category;
    private String spec;
    private String unit;
    private String ybCode;
    private String manufacturer;
    private BigDecimal stockQty;
    private BigDecimal stockAmount;
    private BigDecimal avgPrice;
    private BigDecimal warnQty;
    private String location;
    private Integer status;
    private String remark;
    /** 是否低于库存预警下限 */
    private Boolean warnFlag;
    /** 是否负库存（账实不符） */
    private Boolean abnormalFlag;
    /** 近 30 天出库数量 */
    private BigDecimal outQty30;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
