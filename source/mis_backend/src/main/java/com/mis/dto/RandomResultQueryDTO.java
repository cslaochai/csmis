package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RandomResultQueryDTO extends BizPageParam {

    private String keyword;
    private Long taskId;
    /** 抽取对象类型（1-被检查机构 2-检查人员） */
    private Integer targetType;
    /** 检查结果（1-待检查 2-正常 3-存在问题 4-已整改） */
    private Integer result;
    /** 是否公开（0-未公开 1-已公开） */
    private Integer publicFlag;
}
