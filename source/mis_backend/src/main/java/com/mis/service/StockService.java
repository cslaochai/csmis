package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.StockItemQueryDTO;
import com.mis.dto.StockItemUpsertDTO;
import com.mis.dto.StockOpDTO;
import com.mis.dto.StockTxnQueryDTO;
import com.mis.entity.MisBill;
import com.mis.entity.MisFeeItem;
import com.mis.vo.ActionVO;
import com.mis.vo.StockItemVO;
import com.mis.vo.StockStatsVO;
import com.mis.vo.StockTrendVO;
import com.mis.vo.StockTxnVO;

import java.util.List;

public interface StockService {

    /* ---------------- 库存主档 ---------------- */

    PageVO<StockItemVO> pageItem(StockItemQueryDTO q);

    ActionVO upsertItem(StockItemUpsertDTO dto);

    ActionVO changeItemStatus(Long id, Integer status);

    /* ---------------- 进销存流水 ---------------- */

    PageVO<StockTxnVO> pageTxn(StockTxnQueryDTO q);

    /** 出入库 / 盘点调整 */
    ActionVO operate(StockOpDTO dto);

    /* ---------------- 与结算链路联动 ---------------- */

    /**
     * 结算出库：按结算单药品类明细扣减库存并生成出库流水。
     * 返回实际出库的品项数（未匹配到库存主档的明细跳过）。
     */
    int issueForBill(MisBill bill, List<MisFeeItem> items);

    /**
     * 退费回滚：按结算单号生成红冲入库流水，回补库存。
     */
    int rollbackForBill(String billNo);

    /* ---------------- 统计 ---------------- */

    StockStatsVO stats();

    List<StockTrendVO> trend(int rangeDays);
}
