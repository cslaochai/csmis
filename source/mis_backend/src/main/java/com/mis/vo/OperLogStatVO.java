package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 操作日志统计（审计概览）
 */
@Data
public class OperLogStatVO implements Serializable {

    /** 统计天数 */
    private Integer days;

    /** 日志总条数 */
    private Long total;

    /** 成功条数 */
    private Long successCount;

    /** 失败条数 */
    private Long failCount;

    /** 涉及操作人数 */
    private Long userCount;

    /** 平均耗时（毫秒） */
    private Long avgCostMs;

    /** 按操作类型聚合 */
    private List<DimCount> byType;

    /** 按功能模块聚合 */
    private List<DimCount> byModule;

    /** 按日聚合（用于趋势图） */
    private List<DayCount> byDay;

    @Data
    public static class DimCount implements Serializable {
        private String name;
        private Long count;
    }

    @Data
    public static class DayCount implements Serializable {
        private String day;
        private Long count;
        private Long failCount;
    }
}
