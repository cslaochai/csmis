package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.OperLogQueryDTO;
import com.mis.service.OperLogService;
import com.mis.vo.ActionVO;
import com.mis.vo.OperLogStatVO;
import com.mis.vo.OperLogVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志（审计留痕）
 */
@RestController
@RequestMapping("/system/operlog")
public class OperLogController {

    private final OperLogService operLogService;

    public OperLogController(OperLogService operLogService) {
        this.operLogService = operLogService;
    }

    @GetMapping("/listPage")
    public Result<PageVO<OperLogVO>> listPage(@Valid OperLogQueryDTO q) {
        return Result.success(operLogService.page(q));
    }

    @GetMapping("/stat")
    public Result<OperLogStatVO> stat(@RequestParam(required = false) Integer days) {
        return Result.success(operLogService.stat(days));
    }

    @PostMapping("/clean")
    public Result<ActionVO> clean(@RequestParam(required = false) Integer keepDays) {
        int n = operLogService.clean(keepDays);
        return Result.success(ActionVO.matched(n));
    }
}
