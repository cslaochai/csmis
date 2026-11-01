package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.AgreementQueryDTO;
import com.mis.dto.AgreementStatusDTO;
import com.mis.dto.AgreementUpsertDTO;
import com.mis.service.AgreementService;
import com.mis.vo.ActionVO;
import com.mis.vo.AgreementVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 定点医药机构协议管理
 */
@RestController
@RequestMapping("/agreement")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    @GetMapping("/listPage")
    public Result<PageVO<AgreementVO>> listPage(AgreementQueryDTO query) {
        return Result.success(agreementService.page(query));
    }

    @PostMapping("/upsert")
    public Result<ActionVO> upsert(@RequestBody @Valid AgreementUpsertDTO dto) {
        return Result.success(agreementService.upsert(dto));
    }

    @PostMapping("/status")
    public Result<ActionVO> status(@RequestBody @Valid AgreementStatusDTO dto) {
        return Result.success(agreementService.changeStatus(dto));
    }

    @PostMapping("/assess")
    public Result<ActionVO> assess(@RequestParam Long id, @RequestParam Integer score) {
        return Result.success(agreementService.assess(id, score));
    }
}
