package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.BizPageParam;
import com.mis.vo.ActionVO;
import com.mis.vo.ReconVO;

/**
 * 清算对账
 */
public interface ReconService {

    PageVO<ReconVO> reconPage(BizPageParam query);

    ActionVO doReconcile(String period);
}
