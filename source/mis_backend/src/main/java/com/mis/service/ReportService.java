package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.ReportProcessDTO;
import com.mis.dto.ReportQueryDTO;
import com.mis.dto.ReportRewardDTO;
import com.mis.dto.ReportUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.ReportVO;

public interface ReportService {

    PageVO<ReportVO> page(ReportQueryDTO q);

    ActionVO submit(ReportUpsertDTO dto);

    ActionVO process(ReportProcessDTO dto);

    ActionVO reward(ReportRewardDTO dto);
}
