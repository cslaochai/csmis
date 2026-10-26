package com.mis.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

/**
 * 员工下拉出参：岗位管理选人下拉。
 */
@Data
public class EmployeeSelectVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String empCode;
    private String empName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;
    private String deptName;
    /** 状态（0-停用 1-启用） */
    private Integer status;
}
