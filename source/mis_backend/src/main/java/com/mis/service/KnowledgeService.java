package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.KnowledgeQueryDTO;
import com.mis.dto.KnowledgeUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.KnowledgeVO;

public interface KnowledgeService {

    PageVO<KnowledgeVO> page(KnowledgeQueryDTO q);

    ActionVO upsert(KnowledgeUpsertDTO dto);

    ActionVO changeStatus(Long id, Integer status);

    KnowledgeVO detail(Long id);
}
