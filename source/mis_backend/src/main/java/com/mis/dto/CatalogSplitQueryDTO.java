package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 目录分表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CatalogSplitQueryDTO extends BizPageParam implements Serializable {

    /** 目录类型（drug-药品 treat-诊疗 material-耗材），为空查全部 */
    private String catalogType;

    /** 医保编码 */
    private String ybCode;

    /** 支付类别（甲/乙/丙） */
    private String payClass;

    /** 状态（1-启用 0-停用） */
    private Integer status;
}
