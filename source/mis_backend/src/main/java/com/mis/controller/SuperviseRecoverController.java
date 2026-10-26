package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.RecoverHandleDTO;
import com.mis.dto.RecoverQueryDTO;
import com.mis.service.RecoverService;
import com.mis.vo.ActionVO;
import com.mis.vo.RecoverVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 基金监管 - 违规追回
 */
@RestController
@RequestMapping("/supervise/recover")
@RequiredArgsConstructor
public class SuperviseRecoverController {

    private final RecoverService recoverService;

    @GetMapping("/listPage")
    public Result<PageVO<RecoverVO>> listPage(RecoverQueryDTO query) {
        return Result.success(recoverService.recoverPage(query));
    }

    @GetMapping("/getDetailById")
    public Result<RecoverVO> getDetail(@RequestParam Long id) {
        return Result.success(recoverService.getDetail(id));
    }

    @PostMapping("/recover")
    public Result<ActionVO> recover(@RequestBody @Valid RecoverHandleDTO dto) {
        return Result.success(recoverService.doRecover(dto));
    }

    @PostMapping("/writeOff")
    public Result<ActionVO> writeOff(@RequestBody @Valid RecoverHandleDTO dto) {
        return Result.success(recoverService.writeOff(dto));
    }
}
