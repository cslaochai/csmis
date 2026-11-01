package com.mis.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 信用记分 / 评价入参
 */
@Data
public class CreditScoreDTO implements Serializable {

    private Long id;
    private Integer score;
    private String creditLevel;
    private Integer violationCount;
    private Integer punishType;
    private String remark;
}
