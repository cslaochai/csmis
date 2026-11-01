package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 目录分表统一返回对象（药品/诊疗/耗材同构）
 */
@Data
public class CatalogSplitVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 目录类型（drug / treat / material） */
    private String catalogType;

    /** 目录类型中文 */
    private String catalogTypeName;

    /** 目录编码（药品编码/诊疗编码/耗材编码） */
    private String code;

    /** 名称 */
    private String name;

    /** 通用名（药品） */
    private String genericName;

    /** 剂型（药品） */
    private String dosageForm;

    /** 规格 */
    private String spec;

    /** 强度 */
    private String strength;

    /** 生产企业编码（药品） */
    private String manufacturerCode;

    /** 耗材类型 */
    private String materialType;

    /** 诊疗等级 */
    private String treatLevel;

    /** 医保编码 */
    private String ybCode;

    /** 支付类别（甲/乙/丙） */
    private String payClass;

    /** 自付比例 */
    private BigDecimal selfRatio;

    /** 支付比例 */
    private BigDecimal insRatio;

    /** 医保限价 */
    private BigDecimal limitPrice;

    /** 单位 */
    private String unit;

    /** 状态（1-启用 0-停用） */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
