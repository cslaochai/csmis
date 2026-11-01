package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RandomTaskVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String taskNo;
    private String taskName;
    private Integer checkType;
    private Integer planCount;
    private Integer planCheckerCount;
    private Integer drawCount;
    private Integer checkerCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime drawTime;
    private Integer status;
    private String deptName;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
