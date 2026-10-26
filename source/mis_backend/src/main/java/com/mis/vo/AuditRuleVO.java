package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 监管规则出参：规则配置分页列表行 / 启用规则下拉。
 */
@Data
public class AuditRuleVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 规则编码（唯一） */
    private String ruleCode;
    /** 规则名称 */
    private String ruleName;
    /** 规则类型（duplicate_charge-重复收费 over_limit_price-超限价收费 split_hospitalization-分解住院 bed_hanging-挂床住院 abnormal_cost-高额住院 payclass_c-丙类项目入统筹） */
    private String ruleType;
    /** 稽核对象（1-经办结算单 2-机构报盘） */
    private Integer target;
    /** 违规级别（1-一般 2-较重 3-严重） */
    private Integer severity;
    /** 规则参数（JSON） */
    private String params;
    /** 规则说明 */
    private String description;
    /** 状态（1-启用 0-停用） */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
