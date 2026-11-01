package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DrgGroupUpsertDTO {

    private Long id;
    @NotBlank(message = "病组编码不能为空")
    private String groupCode;
    @NotBlank(message = "病组名称不能为空")
    private String groupName;
    /** 支付方式（1-DRG 2-DIP） */
    @NotNull(message = "支付方式不能为空")
    private Integer payType = 1;
    /** DRG 权重 */
    private BigDecimal weight;
    /** DRG 费率（元/权重） */
    private BigDecimal rate;
    /** DIP 分值 */
    private BigDecimal pointValue;
    /** DIP 每分值成本（元/分） */
    private BigDecimal pointCost;
    /** 组均次费用 */
    private BigDecimal avgCost;
    private Integer caseCount = 0;
    /** 状态（1-启用 0-停用） */
    private Integer status = 1;
    private String description;
}
