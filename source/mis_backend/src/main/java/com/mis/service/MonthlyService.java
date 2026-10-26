package com.mis.service;

import com.mis.common.base.PageResult;
import com.mis.vo.MonthlyResultVO;
import com.mis.vo.MonthlyVO;

/**
 * 月度申报→审核→拨付→年清算
 */
public interface MonthlyService {

    MonthlyResultVO declare(String month);

    MonthlyResultVO audit(String month);

    MonthlyResultVO allocate(String month);

    /** 年终清算：质量保证金考核返还 */
    MonthlyResultVO yearSettle(String month);

    MonthlyResultVO result(String month);

    PageResult<MonthlyVO> page(int pageNum, int pageSize);
}
