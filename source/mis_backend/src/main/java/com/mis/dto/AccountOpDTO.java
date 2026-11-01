package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountOpDTO {

    @NotBlank(message = "身份证号不能为空")
    private String idCard;
    /** 操作类型（3-一次性支取（清退） 4-转移出） */
    @NotNull(message = "操作类型不能为空")
    private Integer opType;
    /** 支取/转出金额 */
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;
    /** 事由（支取需注明：死亡/退休/出国定居等） */
    private String reason;
    /** 关联单号 */
    private String refNo;
}
