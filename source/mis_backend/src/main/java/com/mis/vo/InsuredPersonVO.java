package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 参保人出参：参保查询 / 管理分页列表行 / 新增同步回显。
 */
@Data
public class InsuredPersonVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String idCard;
    private String insuranceNo;
    private String name;
    private String gender;
    /** 险种（2-城镇职工医保 3-城乡居民医保） */
    private String insuranceType;
    /** 参保状态（1-正常参保 0-停保） */
    private Integer personStatus;
    private BigDecimal accountBalance;
    private String region;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
