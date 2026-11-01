package com.mis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ChronicAuditDTO {

    @NotNull(message = "认定单ID不能为空")
    private Long id;
    /** 审核结论（2-认定通过 3-不通过 4-复审通过） */
    @NotNull(message = "审核结论不能为空")
    private Integer result;
    /** 有效期起 */
    private LocalDate validFrom;
    /** 有效期止 */
    private LocalDate validTo;
    /** 审核意见 */
    private String opinion;
}
