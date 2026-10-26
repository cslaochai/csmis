package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 飞检任务详情（任务 + 发现问题明细）
 */
@Data
public class InspectionDetailVO implements Serializable {

    private InspectionVO task;
    private List<InspectionItemVO> items;
    /** 问题涉及金额合计 */
    private BigDecimal totalAmount;
}
