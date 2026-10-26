package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色列表行出参：角色管理分页与下拉（详情见 RoleVO，多 menuIds）。
 */
@Data
public class RoleListVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String roleCode;
    private String roleName;
    /** 角色类型（1-系统角色 2-自定义角色） */
    private Integer roleType;
    /** 数据权限范围（1-全部数据 2-自定义数据 3-本部门数据 5-仅本人数据） */
    private Integer dataScope;
    private Integer sortOrder;
    /** 状态（0-停用 1-启用） */
    private Integer status;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
