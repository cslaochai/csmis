package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点单返回对象
 */
@Data
public class StockCheckVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 盘点单号 */
    private String checkNo;

    /** 定点机构编码 */
    private String medinsCode;

    /** 定点机构名称 */
    private String medinsName;

    /** 盘点日期 */
    private LocalDate checkDate;

    /** 盘点品项数 */
    private Integer itemCount;

    /** 差异品项数 */
    private Integer diffCount;

    /** 差异金额合计 */
    private BigDecimal diffAmount;

    /** 状态（1-待盘点 2-盘点中 3-待复核 4-已办结） */
    private Integer status;

    /** 盘点人 */
    private String checker;

    /** 复核人 */
    private String reviewer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
