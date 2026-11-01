package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.SelfCheckActionDTO;
import com.mis.dto.SelfCheckQueryDTO;
import com.mis.dto.SelfCheckUpsertDTO;
import com.mis.service.SelfCheckService;
import com.mis.vo.ActionVO;
import com.mis.vo.SelfCheckVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 机构自查自纠
 */
@RestController
@RequestMapping("/supervise/selfcheck")
@RequiredArgsConstructor
public class SelfCheckController {

    private final SelfCheckService selfCheckService;

    @GetMapping("/listPage")
    public Result<PageVO<SelfCheckVO>> page(@Valid SelfCheckQueryDTO q) {
        return Result.success(selfCheckService.page(q));
    }

    /** 下发自查自纠任务 */
    @PostMapping("/upsert")
    public Result<ActionVO> upsert(@RequestBody @Valid SelfCheckUpsertDTO dto) {
        return Result.success(selfCheckService.upsert(dto));
    }

    /** 开始自查 */
    @PostMapping("/start")
    public Result<ActionVO> start(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        return Result.success(selfCheckService.start(id));
    }

    /** 机构上报自查/整改结果 */
    @PostMapping("/submit")
    public Result<ActionVO> submit(@RequestBody @Valid SelfCheckActionDTO dto) {
        return Result.success(selfCheckService.submit(dto));
    }

    /** 监管复核 */
    @PostMapping("/review")
    public Result<ActionVO> review(@RequestBody @Valid SelfCheckActionDTO dto) {
        return Result.success(selfCheckService.review(dto));
    }
}
