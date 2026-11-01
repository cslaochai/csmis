package com.mis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * 药品耗材库存主档（进销存）
 * <p>成本口径：移动加权平均单价（每次入库后重算，出库按当前均价结转成本）。
 */
@Data
@TableName("mis_stock_item")
public class MisStockItem implements Serializable {

    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 品项编码（唯一） */
    private String itemCode;
    /** 品项名称 */
    private String itemName;
    /** 类别（西药/中成药/中草药/诊疗项目/医用耗材） */
    private String category;
    /** 规格 */
    private String spec;
    /** 单位 */
    private String unit;
    /** 医保编码（与目录对照） */
    private String ybCode;
    /** 生产企业 */
    private String manufacturer;
    /** 当前库存数量 */
    private BigDecimal stockQty;
    /** 当前库存金额 */
    private BigDecimal stockAmount;
    /** 移动加权平均单价 */
    private BigDecimal avgPrice;
    /** 库存预警下限 */
    private BigDecimal warnQty;
    /** 库房 / 货位 */
    private String location;
    /** 状态（1-启用 0-停用） */
    private Integer status;
    /** 说明 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createById;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateById;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
