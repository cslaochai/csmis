package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.ClaimOpDTO;
import com.mis.dto.ClaimQueryDTO;
import com.mis.dto.ClaimUpsertDTO;
import com.mis.service.ManualClaimService;
import com.mis.vo.ActionVO;
import com.mis.vo.ClaimVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 手工（零星）报销经办
 */
@RestController
@RequestMapping("/claim")
@RequiredArgsConstructor
public class ManualClaimController {

    private final ManualClaimService manualClaimService;

    @GetMapping("/listPage")
    public Result<PageVO<ClaimVO>> listPage(ClaimQueryDTO query) {
        return Result.success(manualClaimService.page(query));
    }

    @PostMapping("/submit")
    public Result<ActionVO> submit(@RequestBody @Valid ClaimUpsertDTO dto) {
        return Result.success(manualClaimService.submit(dto));
    }

    @PostMapping("/audit")
    public Result<ActionVO> audit(@RequestBody @Valid ClaimOpDTO dto) {
        return Result.success(manualClaimService.audit(dto));
    }

    @PostMapping("/pay")
    public Result<ActionVO> pay(@RequestParam Long id) {
        return Result.success(manualClaimService.pay(id));
    }
}
