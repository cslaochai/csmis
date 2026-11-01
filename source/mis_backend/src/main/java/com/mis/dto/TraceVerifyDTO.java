package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TraceVerifyDTO {

    /** 核验单个码 */
    @NotBlank(message = "追溯码不能为空")
    private String code;
    /** 关联结算单号（可选，单码核验时用于比对项目） */
    private String billNo;
    /** 关联项目名称（可选） */
    private String itemName;
    /** 结算日期（用于效期判定） */
    private LocalDate bizDate;
}
