package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.TraceCodeQueryDTO;
import com.mis.dto.TraceCodeUpsertDTO;
import com.mis.dto.TraceLogQueryDTO;
import com.mis.dto.TraceVerifyDTO;
import com.mis.service.TraceService;
import com.mis.vo.ActionVO;
import com.mis.vo.TraceCodeVO;
import com.mis.vo.TraceLogVO;
import com.mis.vo.TraceStatsVO;
import com.mis.vo.TraceVerifyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 药品追溯码监管
 */
@RestController
@RequestMapping("/supervise/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @GetMapping("/code/listPage")
    public Result<PageVO<TraceCodeVO>> pageCode(@Valid TraceCodeQueryDTO q) {
        return Result.success(traceService.pageCode(q));
    }

    /** 登记追溯码（单条或批量） */
    @PostMapping("/code/register")
    public Result<ActionVO> register(@RequestBody TraceCodeUpsertDTO dto) {
        return Result.success(traceService.register(dto));
    }

    /** 单码核验 */
    @PostMapping("/verify")
    public Result<TraceVerifyVO> verify(@RequestBody @Valid TraceVerifyDTO dto) {
        return Result.success(traceService.verify(dto));
    }

    /** 结算单追溯码批量核验 */
    @PostMapping("/verifyBill")
    public Result<ActionVO> verifyBill(@RequestBody Map<String, String> body) {
        return Result.success(traceService.verifyBill(body.get("billNo")));
    }

    @GetMapping("/log/listPage")
    public Result<PageVO<TraceLogVO>> pageLog(@Valid TraceLogQueryDTO q) {
        return Result.success(traceService.pageLog(q));
    }

    @GetMapping("/stats")
    public Result<TraceStatsVO> stats() {
        return Result.success(traceService.stats());
    }
}
