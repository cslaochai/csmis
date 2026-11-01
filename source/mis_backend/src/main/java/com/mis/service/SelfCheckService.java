package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.SelfCheckActionDTO;
import com.mis.dto.SelfCheckQueryDTO;
import com.mis.dto.SelfCheckUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.SelfCheckVO;

public interface SelfCheckService {

    PageVO<SelfCheckVO> page(SelfCheckQueryDTO q);

    /** 下发自查自纠任务 */
    ActionVO upsert(SelfCheckUpsertDTO dto);

    /** 机构开始自查（待自查 → 自查中） */
    ActionVO start(Long id);

    /** 机构上报自查结果（自查中 → 已上报） */
    ActionVO submit(SelfCheckActionDTO dto);

    /** 监管复核（已上报/已整改 → 办结 或 退回整改） */
    ActionVO review(SelfCheckActionDTO dto);
}
