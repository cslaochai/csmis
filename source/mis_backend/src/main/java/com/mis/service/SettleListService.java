package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.SettleListQueryDTO;
import com.mis.entity.MisBill;
import com.mis.entity.MisFeeItem;
import com.mis.vo.ActionVO;
import com.mis.vo.SettleListVO;

import java.util.List;

/**
 * 结算清单主表
 * <p>结算清单是经办机构向医保局报送的费用明细载体，一张结算单对应一张清单；
 * 清单带版本号与上传批次，支撑「清单上传 → 质控 → 退费作废」的业务链路。
 */
public interface SettleListService {

    PageVO<SettleListVO> page(SettleListQueryDTO q);

    /** 按结算单生成结算清单（幂等：同一结算单只生成一张有效清单） */
    ActionVO generate(MisBill bill, List<MisFeeItem> items);

    /** 作废清单（退费冲正时调用） */
    ActionVO voidList(Long billId, String reason);

    /** 回写质控状态（清单质控完成后调用） */
    void updateCheckStatus(Long billId, Integer checkStatus);

    /** 清单详情 */
    SettleListVO detail(Long id);
}
