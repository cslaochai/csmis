package com.mis.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 费用审核流程流转入参（申诉 / 复审 / 合议）
 */
@Data
public class AuditFlowDTO implements Serializable {

    private Long id;
    /** 申诉理由 */
    private String appealReason;
    /** 复审结论（复审通过 / 复审维持） */
    private String reviewResult;
    /** 复审意见 */
    private String reviewOpinion;
    /** 合议意见 */
    private String conferOpinion;
}
