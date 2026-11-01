package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockItemQueryDTO extends BizPageParam {

    private String keyword;
    /** 类别 */
    private String category;
    /** 状态 */
    private Integer status;
    /** 仅看低于预警下限的品项 */
    private Boolean onlyWarn;
    /** 仅看负库存（账实不符） */
    private Boolean onlyAbnormal;
}
