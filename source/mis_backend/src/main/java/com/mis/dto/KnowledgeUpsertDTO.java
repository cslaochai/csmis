package com.mis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识库新增/编辑入参
 */
@Data
public class KnowledgeUpsertDTO implements Serializable {

    private Long id;
    @NotBlank(message = "知识编码不能为空")
    private String kbCode;
    @NotBlank(message = "知识标题不能为空")
    private String kbName;
    @NotBlank(message = "请选择知识类型")
    private String kbType;
    private String content;
    private String source;
    private Integer status;
}
