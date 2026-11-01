package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.FraudClueHandleDTO;
import com.mis.dto.FraudClueQueryDTO;
import com.mis.dto.FraudModelQueryDTO;
import com.mis.dto.FraudModelStatusDTO;
import com.mis.dto.FraudModelUpsertDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.FraudClueVO;
import com.mis.vo.FraudModelVO;

public interface FraudService {

    PageVO<FraudModelVO> pageModel(FraudModelQueryDTO q);

    ActionVO upsertModel(FraudModelUpsertDTO dto);

    ActionVO changeModelStatus(FraudModelStatusDTO dto);

    PageVO<FraudClueVO> pageClue(FraudClueQueryDTO q);

    /** 运行模型（或全部启用模型）生成线索，返回命中条数 */
    ActionVO run(Long modelId);

    ActionVO handleClue(FraudClueHandleDTO dto);
}
