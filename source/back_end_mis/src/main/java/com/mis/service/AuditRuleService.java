package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.RuleQueryDTO;
import com.mis.dto.RuleStatusDTO;
import com.mis.dto.RuleUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.AuditRuleVO;

import java.util.List;

/**
 * 监管规则
 */
public interface AuditRuleService {

    PageVO<AuditRuleVO> rulePage(RuleQueryDTO query);

    ActionVO upsert(RuleUpsertDTO dto);

    ActionVO changeStatus(RuleStatusDTO dto);

    List<AuditRuleVO> enabledRules();
}
