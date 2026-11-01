package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.StockCheckItemDTO;
import com.mis.dto.StockCheckQueryDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.StockCheckItemVO;
import com.mis.vo.StockCheckVO;

import java.util.List;

/**
 * 库存盘点（与进销存联动）
 * <p>流程：生成盘点单（按当前库存快照带出账面数）→ 录入实盘数 → 生成差异 →
 * 差异调整库存（生成盘点调整流水）→ 复核办结。
 */
public interface StockCheckService {

    PageVO<StockCheckVO> page(StockCheckQueryDTO q);

    /** 生成盘点单：按当前库存快照带出账面数，状态=待盘点(1) */
    ActionVO create(String medinsCode, String medinsName, String checkDate);

    /** 盘点单明细（账面 + 实盘 + 差异） */
    List<StockCheckItemVO> items(String checkNo);

    /** 录入实盘数 */
    ActionVO submitItem(StockCheckItemDTO dto);

    /** 批量提交实盘（前端一次提交多个品项） */
    ActionVO submitItems(List<StockCheckItemDTO> list);

    /** 完成盘点：生成差异并调整库存，状态=待复核(3) */
    ActionVO finish(String checkNo);

    /** 复核：确认差异调整，状态=已办结(4)；驳回则退回待盘点 */
    ActionVO review(String checkNo, boolean pass, String opinion);
}
