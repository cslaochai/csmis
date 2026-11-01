package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 信用管理出参
 */
@Data
public class CreditVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Integer subjectType;
    private String subjectCode;
    private String subjectName;
    private String creditLevel;
    private Integer score;
    private Integer violationCount;
    private Integer punishType;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
