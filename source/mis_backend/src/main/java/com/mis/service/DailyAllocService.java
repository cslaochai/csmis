package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.DailyQueryDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.DailyAllocVO;

public interface DailyAllocService {

    /** 生成某业务日的按笔日拨付批次（T+1 拨付，比例默认 60%） */
    ActionVO generate(String bizDate);

    PageVO<DailyAllocVO> page(DailyQueryDTO q);

    /** 银行电子回单回传：标记已回单 */
    ActionVO remit(Long id);
}
