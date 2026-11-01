package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportUpsertDTO {

    private Long id;
    private String reporterName;
    private String reporterContact;
    private String reportedMedinsCode;
    private String reportedMedinsName;
    private String reportedSubject;
    /** 问题类型 */
    private String problemType;
    @NotBlank(message = "举报内容不能为空")
    private String content;
    private String attachUrl;
}
