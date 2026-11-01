package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志返回对象
 */
@Data
public class OperLogVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 操作类型（新增/修改/删除/查询/审核/导出/其他） */
    private String operType;

    /** 功能模块 */
    private String operModule;

    /** 操作描述 */
    private String operDesc;

    /** 操作人 */
    private String operUser;

    /** 操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operUserId;

    /** 请求地址 */
    private String requestUri;

    /** 请求方式 */
    private String requestMethod;

    /** 请求参数（截断） */
    private String requestParams;

    /** 业务对象类型 */
    private String targetType;

    /** 业务对象ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    /** 业务对象编号 */
    private String targetNo;

    /** 操作IP */
    private String operIp;

    /** 耗时（毫秒） */
    private Long costMs;

    /** 结果（1-成功 0-失败） */
    private Integer operResult;

    /** 错误信息 */
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
