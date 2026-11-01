package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.PrecheckResultHandleDTO;
import com.mis.dto.PrecheckResultQueryDTO;
import com.mis.dto.PrecheckRuleQueryDTO;
import com.mis.dto.PrecheckRuleStatusDTO;
import com.mis.dto.PrecheckRuleUpsertDTO;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MisPendingSettle;
import com.mis.vo.ActionVO;
import com.mis.vo.DecVO;
import com.mis.vo.PrecheckResultVO;
import com.mis.vo.PrecheckRuleVO;
import com.mis.vo.PrecheckVO;

import java.util.List;

public interface PrecheckService {

    PageVO<PrecheckRuleVO> pageRule(PrecheckRuleQueryDTO q);

    ActionVO upsertRule(PrecheckRuleUpsertDTO dto);

    ActionVO changeRuleStatus(PrecheckRuleStatusDTO dto);

    PageVO<PrecheckResultVO> pageResult(PrecheckResultQueryDTO q);

    /** 运行启用的规则，对待结算业务真实评估并生成命中结果，返回命中条数 */
    ActionVO evaluate();

    ActionVO handleResult(PrecheckResultHandleDTO dto);

    /** 事前提醒：结算前实时比对，只提醒不阻断（不落库） */
    PrecheckVO check(MisPendingSettle pending, List<MisFeeItem> items, DecVO dec);

    /** 事前提醒（按业务单号）：自动装载待结算业务与费用明细后评估 */
    PrecheckVO checkByBizNo(String bizNo);

    /** 事中拦截：结算关口实时判定，命中阻断项即拒付，并落库留痕 */
    PrecheckVO intercept(MisPendingSettle pending, List<MisFeeItem> items, DecVO dec);
}
