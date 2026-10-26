package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.RecoverHandleDTO;
import com.mis.dto.RecoverQueryDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.RecoverVO;

/**
 * 违规追回
 */
public interface RecoverService {

    PageVO<RecoverVO> recoverPage(RecoverQueryDTO query);

    RecoverVO getDetail(Long id);

    /** 确认追回：待追回 → 已追回 */
    ActionVO doRecover(RecoverHandleDTO dto);

    /** 核销：待追回 → 已核销 */
    ActionVO writeOff(RecoverHandleDTO dto);
}
