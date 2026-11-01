package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 信用主体新增 / 编辑入参
 */
@Data
public class CreditUpsertDTO implements Serializable {

    private Long id;
    private Integer subjectType;
    @NotBlank(message = "主体编码不能为空")
    private String subjectCode;
    @NotBlank(message = "主体名称不能为空")
    private String subjectName;
    private String creditLevel;
    private Integer score;
    private Integer violationCount;
    private Integer punishType;
    private String remark;
}
