package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.FilingQueryDTO;
import com.mis.dto.FilingUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.FilingVO;

/**
 * 异地就医备案
 */
public interface FilingService {

    PageVO<FilingVO> filingPage(FilingQueryDTO query);

    ActionVO upsert(FilingUpsertDTO dto);

    ActionVO revoke(Long id);
}
