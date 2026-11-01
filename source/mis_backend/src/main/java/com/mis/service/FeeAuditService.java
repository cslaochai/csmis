package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.AuditQueryDTO;
import com.mis.dto.AuditFlowDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.FeeAuditVO;

/**
 * 费用合规性审核
 */
public interface FeeAuditService {

    PageVO<FeeAuditVO> auditPage(AuditQueryDTO query);

    ActionVO pass(Long id);

    ActionVO reject(Long id);

    /** 机构申诉（仅被驳回可申诉） */
    ActionVO appeal(AuditFlowDTO dto);

    /** 复审（仅申诉中可复审，结论：复审通过 / 复审维持） */
    ActionVO review(AuditFlowDTO dto);

    /** 合议终裁（申诉中 / 复审后均可合议） */
    ActionVO confer(AuditFlowDTO dto);
}
