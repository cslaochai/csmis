package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.ClaimOpDTO;
import com.mis.dto.ClaimQueryDTO;
import com.mis.dto.ClaimUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.ClaimVO;

public interface ManualClaimService {

    PageVO<ClaimVO> page(ClaimQueryDTO q);

    ActionVO submit(ClaimUpsertDTO dto);

    /** 审核：passed=true 核定统筹支付并置已审核；passed=false 退回 */
    ActionVO audit(ClaimOpDTO dto);

    /** 拨付：已审核 → 已拨付 */
    ActionVO pay(Long id);
}
