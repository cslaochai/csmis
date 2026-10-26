package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典类型出参：类型分页/下拉/编辑回显。
 */
@Data
public class DictTypeVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String dictType;
    private String dictName;
    /** 状态（0-停用 1-启用） */
    private Integer status;
    /** 来源（1-系统级 2-自定义） */
    private Integer dictSource;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    private String remark;
}
