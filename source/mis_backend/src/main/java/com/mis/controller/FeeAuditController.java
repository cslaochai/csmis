package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.AuditFlowDTO;
import com.mis.dto.AuditQueryDTO;
import com.mis.dto.IdDTO;
import com.mis.service.FeeAuditService;
import com.mis.vo.ActionVO;
import com.mis.vo.FeeAuditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 费用合规性审核
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class FeeAuditController {

    private final FeeAuditService feeAuditService;

    @GetMapping("/listPage")
    public Result<PageVO<FeeAuditVO>> listPage(AuditQueryDTO query) {
        return Result.success(feeAuditService.auditPage(query));
    }

    @PostMapping("/pass")
    public Result<ActionVO> pass(@RequestBody IdDTO dto) {
        return Result.success(feeAuditService.pass(dto.getId()));
    }

    @PostMapping("/reject")
    public Result<ActionVO> reject(@RequestBody IdDTO dto) {
        return Result.success(feeAuditService.reject(dto.getId()));
    }

    @PostMapping("/appeal")
    public Result<ActionVO> appeal(@RequestBody AuditFlowDTO dto) {
        return Result.success(feeAuditService.appeal(dto));
    }

    @PostMapping("/review")
    public Result<ActionVO> review(@RequestBody AuditFlowDTO dto) {
        return Result.success(feeAuditService.review(dto));
    }

    @PostMapping("/confer")
    public Result<ActionVO> confer(@RequestBody AuditFlowDTO dto) {
        return Result.success(feeAuditService.confer(dto));
    }
}
