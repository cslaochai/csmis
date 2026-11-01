package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.DrgCaseOpDTO;
import com.mis.dto.DrgCaseQueryDTO;
import com.mis.dto.DrgGroupQueryDTO;
import com.mis.dto.DrgGroupUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.DrgCaseVO;
import com.mis.vo.DrgGroupVO;
import com.mis.vo.DrgStatsVO;

public interface DrgService {

    /* ---------------- 病组 / 病种池 ---------------- */

    PageVO<DrgGroupVO> pageGroup(DrgGroupQueryDTO q);

    ActionVO upsertGroup(DrgGroupUpsertDTO dto);

    ActionVO changeGroupStatus(Long id, Integer status);

    /* ---------------- 病例入组与监管 ---------------- */

    PageVO<DrgCaseVO> pageCase(DrgCaseQueryDTO q);

    /** 对已结算住院病例批量入组并做偏差/超支判定，返回新增入组条数 */
    ActionVO groupCases();

    /** 病例处置（特例单议 / 恢复正常） */
    ActionVO handleCase(DrgCaseOpDTO dto);

    /** 监管看板统计 */
    DrgStatsVO stats();
}
