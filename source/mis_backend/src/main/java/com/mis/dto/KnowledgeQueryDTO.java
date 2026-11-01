package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeQueryDTO extends BizPageParam {

    /** 知识类型 */
    private String kbType;
    /** 状态（1-启用 0-停用） */
    private Integer status;
}
