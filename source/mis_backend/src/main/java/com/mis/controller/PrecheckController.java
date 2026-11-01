package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.BizNoDTO;
import com.mis.dto.PrecheckResultHandleDTO;
import com.mis.dto.PrecheckResultQueryDTO;
import com.mis.dto.PrecheckRuleQueryDTO;
import com.mis.dto.PrecheckRuleStatusDTO;
import com.mis.dto.PrecheckRuleUpsertDTO;
import com.mis.service.PrecheckService;
import com.mis.vo.ActionVO;
import com.mis.vo.PrecheckResultVO;
import com.mis.vo.PrecheckRuleVO;
import com.mis.vo.PrecheckVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supervise/precheck")
public class PrecheckController {

    private final PrecheckService precheckService;

    public PrecheckController(PrecheckService precheckService) {
        this.precheckService = precheckService;
    }

    @GetMapping("/rule/listPage")
    public Result<PageVO<PrecheckRuleVO>> pageRule(@Valid PrecheckRuleQueryDTO q) {
        return Result.success(precheckService.pageRule(q));
    }

    @PostMapping("/rule/upsert")
    public Result<ActionVO> upsertRule(@RequestBody @Valid PrecheckRuleUpsertDTO dto) {
        return Result.success(precheckService.upsertRule(dto));
    }

    @PostMapping("/rule/status")
    public Result<ActionVO> changeRuleStatus(@RequestBody @Valid PrecheckRuleStatusDTO dto) {
        return Result.success(precheckService.changeRuleStatus(dto));
    }

    @GetMapping("/result/listPage")
    public Result<PageVO<PrecheckResultVO>> pageResult(@Valid PrecheckResultQueryDTO q) {
        return Result.success(precheckService.pageResult(q));
    }

    @PostMapping("/result/evaluate")
    public Result<ActionVO> evaluate() {
        return Result.success(precheckService.evaluate());
    }

    /** 结算前事前提醒（按业务单号，实时比对，不落库） */
    @PostMapping("/result/check")
    public Result<PrecheckVO> check(@RequestBody @Valid BizNoDTO dto) {
        return Result.success(precheckService.checkByBizNo(dto.getBizNo()));
    }

    @PostMapping("/result/handle")
    public Result<ActionVO> handleResult(@RequestBody @Valid PrecheckResultHandleDTO dto) {
        return Result.success(precheckService.handleResult(dto));
    }
}
