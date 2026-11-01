package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.DailyQueryDTO;
import com.mis.service.DailyAllocService;
import com.mis.vo.ActionVO;
import com.mis.vo.DailyAllocVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 医保结算 - 日拨付（按笔）管理
 */
@RestController
@RequestMapping("/settle/daily")
@RequiredArgsConstructor
public class DailyAllocController {

    private final DailyAllocService dailyAllocService;

    @PostMapping("/generate")
    public Result<ActionVO> generate(@RequestParam String bizDate) {
        return Result.success(dailyAllocService.generate(bizDate));
    }

    @GetMapping("/listPage")
    public Result<PageVO<DailyAllocVO>> listPage(DailyQueryDTO query) {
        return Result.success(dailyAllocService.page(query));
    }

    @PostMapping("/remit")
    public Result<ActionVO> remit(@RequestParam Long id) {
        return Result.success(dailyAllocService.remit(id));
    }
}
