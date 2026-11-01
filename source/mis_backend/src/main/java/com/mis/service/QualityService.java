package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.QualityResultHandleDTO;
import com.mis.dto.QualityResultQueryDTO;
import com.mis.dto.QualityRuleQueryDTO;
import com.mis.dto.QualityRuleUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.QualityResultVO;
import com.mis.vo.QualityRuleVO;
import com.mis.vo.QualityStatsVO;

public interface QualityService {

    /* ---------------- 质控规则 ---------------- */

    PageVO<QualityRuleVO> pageRule(QualityRuleQueryDTO q);

    ActionVO upsertRule(QualityRuleUpsertDTO dto);

    ActionVO changeRuleStatus(Long id, Integer status);

    /* ---------------- 质控结果 ---------------- */

    PageVO<QualityResultVO> pageResult(QualityResultQueryDTO q);

    /** 对结算单清单批量质控，命中留痕（按 结算单+规则+明细 幂等去重） */
    ActionVO check();

    /** 质控结果处理（确认/整改/忽略），确认时可联动生成违规追回扣款单 */
    ActionVO handleResult(QualityResultHandleDTO dto);

    /** 质控看板统计 */
    QualityStatsVO stats();
}
