package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.ChronicAuditDTO;
import com.mis.dto.ChronicQueryDTO;
import com.mis.dto.ChronicUpsertDTO;
import com.mis.service.ChronicService;
import com.mis.vo.ActionVO;
import com.mis.vo.ChronicVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 门诊慢特病待遇认定
 */
@RestController
@RequestMapping("/insure/chronic")
@RequiredArgsConstructor
public class ChronicController {

    private final ChronicService chronicService;

    @GetMapping("/listPage")
    public Result<PageVO<ChronicVO>> listPage(@Valid ChronicQueryDTO q) {
        return Result.success(chronicService.page(q));
    }

    /** 申请认定 */
    @PostMapping("/upsert")
    public Result<ActionVO> upsert(@RequestBody @Valid ChronicUpsertDTO dto) {
        return Result.success(chronicService.upsert(dto));
    }

    /** 待遇认定审核 */
    @PostMapping("/audit")
    public Result<ActionVO> audit(@RequestBody @Valid ChronicAuditDTO dto) {
        return Result.success(chronicService.audit(dto));
    }
}
