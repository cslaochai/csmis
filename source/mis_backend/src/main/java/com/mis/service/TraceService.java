package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.TraceCodeQueryDTO;
import com.mis.dto.TraceCodeUpsertDTO;
import com.mis.dto.TraceLogQueryDTO;
import com.mis.dto.TraceVerifyDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.TraceCodeVO;
import com.mis.vo.TraceLogVO;
import com.mis.vo.TraceStatsVO;
import com.mis.vo.TraceVerifyVO;

public interface TraceService {

    /* ---------------- 追溯码台账 ---------------- */

    PageVO<TraceCodeVO> pageCode(TraceCodeQueryDTO q);

    /** 登记追溯码（单条或批量），重复码自动跳过并返回跳过数量 */
    ActionVO register(TraceCodeUpsertDTO dto);

    /* ---------------- 核验 ---------------- */

    /** 单码核验 */
    TraceVerifyVO verify(TraceVerifyDTO dto);

    /** 对结算单的药品类明细批量核验追溯码 */
    ActionVO verifyBill(String billNo);

    /* ---------------- 核验日志 ---------------- */

    PageVO<TraceLogVO> pageLog(TraceLogQueryDTO q);

    TraceStatsVO stats();
}
