package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 员工资格证书出参：员工档案资格证书列表行。
 */
@Data
public class QualificationVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;
    /** 证书类型（his_emp_cert_type 字典） */
    private String certType;
    private String certNo;
    /** 发证机关（his_emp_cert_org 字典） */
    private String issueOrg;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;
    /** 有效期至（空=长期有效） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validUntil;
    private String remark;
}
