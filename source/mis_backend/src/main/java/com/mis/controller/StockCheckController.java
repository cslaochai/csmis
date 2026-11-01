package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.StockCheckItemDTO;
import com.mis.dto.StockCheckQueryDTO;
import com.mis.service.StockCheckService;
import com.mis.vo.ActionVO;
import com.mis.vo.StockCheckItemVO;
import com.mis.vo.StockCheckVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存盘点（与进销存联动）
 */
@RestController
@RequestMapping("/stock/check")
@RequiredArgsConstructor
public class StockCheckController {

    private final StockCheckService stockCheckService;

    @GetMapping("/listPage")
    public Result<PageVO<StockCheckVO>> listPage(@Valid StockCheckQueryDTO q) {
        return Result.success(stockCheckService.page(q));
    }

    /** 生成盘点单（按当前库存快照带出账面数） */
    @PostMapping("/create")
    public Result<ActionVO> create(@RequestParam(required = false) String medinsCode,
                                   @RequestParam(required = false) String medinsName,
                                   @RequestParam(required = false) String checkDate) {
        return Result.success(stockCheckService.create(medinsCode, medinsName, checkDate));
    }

    /** 盘点单明细 */
    @GetMapping("/items")
    public Result<List<StockCheckItemVO>> items(@RequestParam String checkNo) {
        return Result.success(stockCheckService.items(checkNo));
    }

    /** 录入实盘数 */
    @PostMapping("/submitItem")
    public Result<ActionVO> submitItem(@RequestBody @Valid StockCheckItemDTO dto) {
        return Result.success(stockCheckService.submitItem(dto));
    }

    /** 批量录入实盘数 */
    @PostMapping("/submitItems")
    public Result<ActionVO> submitItems(@RequestBody List<@Valid StockCheckItemDTO> list) {
        return Result.success(stockCheckService.submitItems(list));
    }

    /** 完成盘点：计算差异并调整库存 */
    @PostMapping("/finish")
    public Result<ActionVO> finish(@RequestParam String checkNo) {
        return Result.success(stockCheckService.finish(checkNo));
    }

    /** 复核：确认差异调整 / 驳回 */
    @PostMapping("/review")
    public Result<ActionVO> review(@RequestParam String checkNo,
                                   @RequestParam boolean pass,
                                   @RequestParam(required = false) String opinion) {
        return Result.success(stockCheckService.review(checkNo, pass, opinion));
    }
}
