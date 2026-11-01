package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 定点协议新增/编辑入参
 */
@Data
public class AgreementUpsertDTO implements Serializable {

    private Long id;
    @NotBlank(message = "机构编码不能为空")
    private String medinsCode;
    @NotBlank(message = "机构名称不能为空")
    private String medinsName;
    private Integer medinsType;
    private String agreementNo;
    private LocalDate signDate;
    private LocalDate expireDate;
    private String serviceScope;
    private String remark;
}
