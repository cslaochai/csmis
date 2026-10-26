package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 经办科室出参：岗位配置下拉 / 科室树与编辑回显。
 */
@Data
public class DepartmentVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String deptCode;
    private String deptName;
    /** 科室类型（1-前台经办 2-审核稽核 3-基金财务 4-综合管理） */
    private Integer deptType;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private Integer sortOrder;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptLeaderId;
    private String phone;
    private String email;
    /** 状态（0-停用 1-启用） */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    private String remark;

    /** 子节点（树形返回用，非表字段） */
    private List<DepartmentVO> children;
}
