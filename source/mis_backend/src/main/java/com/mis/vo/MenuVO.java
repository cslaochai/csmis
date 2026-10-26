package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单出参：登录后菜单树 / 菜单管理树与编辑回显。
 */
@Data
public class MenuVO implements Serializable {

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    private String remark;

    /** 子菜单（树形返回用，非表字段） */
    private List<MenuVO> children;
}
