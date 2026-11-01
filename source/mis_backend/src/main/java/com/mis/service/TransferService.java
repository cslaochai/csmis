package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.TransferHandleDTO;
import com.mis.dto.TransferQueryDTO;
import com.mis.dto.TransferUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.TransferVO;

public interface TransferService {

    PageVO<TransferVO> page(TransferQueryDTO q);

    /** 发起转移接续申请 */
    ActionVO upsert(TransferUpsertDTO dto);

    /**
     * 转移处置：
     * 2-受理；3-转出；4-转入；5-完成转移（结转个人账户余额与缴费年限）。
     */
    ActionVO handle(TransferHandleDTO dto);
}
