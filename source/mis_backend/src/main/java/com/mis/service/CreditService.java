package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.CreditQueryDTO;
import com.mis.dto.CreditScoreDTO;
import com.mis.dto.CreditUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.CreditVO;

public interface CreditService {

    PageVO<CreditVO> page(CreditQueryDTO q);

    ActionVO upsert(CreditUpsertDTO dto);

    /** 记分 / 评价：更新评分、等级、违规次数、奖惩类型 */
    ActionVO record(CreditScoreDTO dto);
}
