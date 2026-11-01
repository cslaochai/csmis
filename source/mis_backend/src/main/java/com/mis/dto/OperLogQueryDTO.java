package com.mis.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogQueryDTO extends BizPageParam {

    /** 操作类型 */
    private String operType;

    /** 功能模块 */
    private String operModule;

    /** 操作人 */
    private String operUser;

    /** 业务对象类型 */
    private String targetType;

    /** 业务对象编号 */
    private String targetNo;

    /** 结果（1-成功 0-失败） */
    private Integer operResult;

    /** 请求地址（模糊） */
    private String requestUri;

    /** 起始日期 yyyy-MM-dd */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "起始日期格式应为 yyyy-MM-dd")
    private String beginDate;

    /** 截止日期 yyyy-MM-dd */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "截止日期格式应为 yyyy-MM-dd")
    private String endDate;
}
