package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.StockItemQueryDTO;
import com.mis.dto.StockItemUpsertDTO;
import com.mis.dto.StockOpDTO;
import com.mis.dto.StockTxnQueryDTO;
import com.mis.service.StockService;
import com.mis.vo.ActionVO;
import com.mis.vo.StockItemVO;
import com.mis.vo.StockStatsVO;
import com.mis.vo.StockTrendVO;
import com.mis.vo.StockTxnVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 药品耗材进销存
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /* ---------------- 库存主档 ---------------- */

    @GetMapping("/item/listPage")
    public Result<PageVO<StockItemVO>> pageItem(@Valid StockItemQueryDTO q) {
        return Result.success(stockService.pageItem(q));
    }

    @PostMapping("/item/upsert")
    public Result<ActionVO> upsertItem(@RequestBody @Valid StockItemUpsertDTO dto) {
        return Result.success(stockService.upsertItem(dto));
    }

    @PostMapping("/item/status")
    public Result<ActionVO> changeItemStatus(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        Integer status = Integer.valueOf(String.valueOf(body.get("status")));
        return Result.success(stockService.changeItemStatus(id, status));
    }

    /* ---------------- 进销存流水 ---------------- */

    @GetMapping("/txn/listPage")
    public Result<PageVO<StockTxnVO>> pageTxn(@Valid StockTxnQueryDTO q) {
        return Result.success(stockService.pageTxn(q));
    }

    /** 出入库 / 盘点调整 */
    @PostMapping("/txn/operate")
    public Result<ActionVO> operate(@RequestBody @Valid StockOpDTO dto) {
        return Result.success(stockService.operate(dto));
    }

    /* ---------------- 统计 ---------------- */

    @GetMapping("/stats")
    public Result<StockStatsVO> stats() {
        return Result.success(stockService.stats());
    }

    @GetMapping("/trend")
    public Result<List<StockTrendVO>> trend(@RequestParam(defaultValue = "30") int rangeDays) {
        return Result.success(stockService.trend(rangeDays));
    }
}
