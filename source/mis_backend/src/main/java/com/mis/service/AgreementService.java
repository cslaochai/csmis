package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.AgreementQueryDTO;
import com.mis.dto.AgreementStatusDTO;
import com.mis.dto.AgreementUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.AgreementVO;

public interface AgreementService {

    PageVO<AgreementVO> page(AgreementQueryDTO q);

    ActionVO upsert(AgreementUpsertDTO dto);

    ActionVO changeStatus(AgreementStatusDTO dto);

    ActionVO assess(Long id, Integer score);
}
