package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.ChronicAuditDTO;
import com.mis.dto.ChronicQueryDTO;
import com.mis.dto.ChronicUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.ChronicVO;

public interface ChronicService {

    PageVO<ChronicVO> page(ChronicQueryDTO q);

    /** 申请门诊慢特病待遇认定 */
    ActionVO upsert(ChronicUpsertDTO dto);

    /** 待遇认定审核（2-认定通过 3-不通过 4-复审通过） */
    ActionVO audit(ChronicAuditDTO dto);
}
