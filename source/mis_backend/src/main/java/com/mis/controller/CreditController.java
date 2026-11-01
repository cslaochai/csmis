package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.CreditQueryDTO;
import com.mis.dto.CreditScoreDTO;
import com.mis.dto.CreditUpsertDTO;
import com.mis.service.CreditService;
import com.mis.vo.ActionVO;
import com.mis.vo.CreditVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 基金监管 - 信用管理
 */
@RestController
@RequestMapping("/supervise/credit")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/listPage")
    public Result<PageVO<CreditVO>> listPage(CreditQueryDTO query) {
        return Result.success(creditService.page(query));
    }

    @PostMapping("/upsert")
    public Result<ActionVO> upsert(@RequestBody @Valid CreditUpsertDTO dto) {
        return Result.success(creditService.upsert(dto));
    }

    @PostMapping("/record")
    public Result<ActionVO> record(@RequestBody @Valid CreditScoreDTO dto) {
        return Result.success(creditService.record(dto));
    }
}
