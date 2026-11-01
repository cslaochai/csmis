package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.QualityResultHandleDTO;
import com.mis.dto.QualityResultQueryDTO;
import com.mis.dto.QualityRuleQueryDTO;
import com.mis.dto.QualityRuleUpsertDTO;
import com.mis.service.QualityService;
import com.mis.vo.ActionVO;
import com.mis.vo.QualityResultVO;
import com.mis.vo.QualityRuleVO;
import com.mis.vo.QualityStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 结算清单质控
 */
@RestController
@RequestMapping("/supervise/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;

    @GetMapping("/rule/listPage")
    public Result<PageVO<QualityRuleVO>> pageRule(@Valid QualityRuleQueryDTO q) {
        return Result.success(qualityService.pageRule(q));
    }

    @PostMapping("/rule/upsert")
    public Result<ActionVO> upsertRule(@RequestBody @Valid QualityRuleUpsertDTO dto) {
        return Result.success(qualityService.upsertRule(dto));
    }

    @PostMapping("/rule/status")
    public Result<ActionVO> changeRuleStatus(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        Integer status = Integer.valueOf(String.valueOf(body.get("status")));
        return Result.success(qualityService.changeRuleStatus(id, status));
    }

    @GetMapping("/result/listPage")
    public Result<PageVO<QualityResultVO>> pageResult(@Valid QualityResultQueryDTO q) {
        return Result.success(qualityService.pageResult(q));
    }

    /** 执行结算清单批量质控 */
    @PostMapping("/result/check")
    public Result<ActionVO> check() {
        return Result.success(qualityService.check());
    }

    /** 质控结果处理（确认/整改/忽略，可联动生成扣款单） */
    @PostMapping("/result/handle")
    public Result<ActionVO> handleResult(@RequestBody @Valid QualityResultHandleDTO dto) {
        return Result.success(qualityService.handleResult(dto));
    }

    @GetMapping("/stats")
    public Result<QualityStatsVO> stats() {
        return Result.success(qualityService.stats());
    }
}
