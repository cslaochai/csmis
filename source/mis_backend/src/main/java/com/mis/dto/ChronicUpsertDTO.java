package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ChronicUpsertDTO {

    private Long id;
    @NotBlank(message = "参保人姓名不能为空")
    private String personName;
    @NotBlank(message = "身份证号不能为空")
    private String idCard;
    private String insuranceNo;
    @NotBlank(message = "病种名称不能为空")
    private String diseaseName;
    /** 病种编码 */
    private String diseaseCode;
    /** 待遇类别（1-门诊特殊病种 2-门诊慢特病） */
    @NotNull(message = "待遇类别不能为空")
    private Integer diseaseLevel = 2;
    /** 申请医师 */
    private String doctor;
    /** 定点医疗机构 */
    private String hospital;
    private LocalDate applyDate;
    private String remark;
}
