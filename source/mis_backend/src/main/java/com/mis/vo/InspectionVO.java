package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 飞检任务出参：任务列表行（详情见 InspectionDetailVO）。
 */
@Data
public class InspectionVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 任务单号（唯一） */
    private String inspectionNo;
    /** 任务名称 */
    private String title;
    /** 检查类型（专项检查/日常巡查/举报核查） */
    private String inspectType;
    /** 被查机构 */
    private String targetOrg;
    /** 计划开始日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planStart;
    /** 计划结束日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planEnd;
    /** 带队人 */
    private String leader;
    /** 检查组成员 */
    private String members;
    /** 检查范围 */
    private String scopeDesc;
    /** 状态（草稿/待开展/检查中/已完成） */
    private String status;
    /** 检查结果小结 */
    private String resultSummary;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
