package com.mis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 菜单（仿 HIS sys_menu）
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String menuName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private Integer sortOrder;
    /** 菜单类型（1-目录 2-菜单 3-按钮） */
    private Integer menuType;
    private String path;
    private String component;
    private String menuKey;
    private String icon;
    private String permission;
    /** 是否外链（0-否 1-是） */
    private Integer isFrame;
    /** 是否缓存（0-否 1-是） */
    private Integer isCache;
    /** 是否可见（0-隐藏 1-显示） */
    private Integer isVisible;
    /** 状态（0-停用 1-启用） */
    private Integer status;

    @TableLogic
    private Integer delFlag;
    private String remark;

    /** 子节点（树形返回用，非表字段） */
    @TableField(exist = false)
    private List<SysMenu> children;

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
