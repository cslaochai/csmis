package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.TransferHandleDTO;
import com.mis.dto.TransferQueryDTO;
import com.mis.dto.TransferUpsertDTO;
import com.mis.service.TransferService;
import com.mis.vo.ActionVO;
import com.mis.vo.TransferVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 医保关系转移接续
 */
@RestController
@RequestMapping("/insure/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping("/listPage")
    public Result<PageVO<TransferVO>> listPage(@Valid TransferQueryDTO q) {
        return Result.success(transferService.page(q));
    }

    /** 发起转移接续申请 */
    @PostMapping("/upsert")
    public Result<ActionVO> upsert(@RequestBody @Valid TransferUpsertDTO dto) {
        return Result.success(transferService.upsert(dto));
    }

    /** 受理 / 转出 / 转入 / 完成转移 */
    @PostMapping("/handle")
    public Result<ActionVO> handle(@RequestBody @Valid TransferHandleDTO dto) {
        return Result.success(transferService.handle(dto));
    }
}
