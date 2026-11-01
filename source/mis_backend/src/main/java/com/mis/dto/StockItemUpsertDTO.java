package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockItemUpsertDTO {

    private Long id;
    @NotBlank(message = "品项编码不能为空")
    private String itemCode;
    @NotBlank(message = "品项名称不能为空")
    private String itemName;
    /** 类别 */
    private String category;
    private String spec;
    private String unit;
    private String ybCode;
    private String manufacturer;
    /** 库存预警下限 */
    private BigDecimal warnQty = BigDecimal.ZERO;
    private String location;
    private Integer status = 1;
    private String remark;
}
