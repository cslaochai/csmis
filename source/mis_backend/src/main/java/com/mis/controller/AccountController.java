package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.AccountOpDTO;
import com.mis.dto.AccountTxnQueryDTO;
import com.mis.service.AccountService;
import com.mis.vo.AccountInfoVO;
import com.mis.vo.AccountTxnVO;
import com.mis.vo.ActionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 个人账户管理
 */
@RestController
@RequestMapping("/insure/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/listPage")
    public Result<PageVO<AccountTxnVO>> listPage(@Valid AccountTxnQueryDTO q) {
        return Result.success(accountService.page(q));
    }

    /** 个人账户查询（余额 + 流水明细） */
    @GetMapping("/info")
    public Result<AccountInfoVO> info(@RequestParam String idCard) {
        return Result.success(accountService.info(idCard));
    }

    /** 一次性支取（清退）/ 转移出 */
    @PostMapping("/operate")
    public Result<ActionVO> operate(@RequestBody @Valid AccountOpDTO dto) {
        return Result.success(accountService.operate(dto));
    }

    /** 从参保缴费记录划入个人账户（幂等，已划入不重复入账） */
    @PostMapping("/accrue")
    public Result<ActionVO> accrue(@RequestParam String idCard,
                                    @RequestParam(required = false) List<Long> payRecordIds) {
        return Result.success(accountService.accrueFromPayRecord(idCard, payRecordIds));
    }

    /** 批量划入：按缴费月份生成个人账户划入流水 */
    @PostMapping("/accrueBatch")
    public Result<ActionVO> accrueBatch(@RequestParam(required = false) String payMonth) {
        return Result.success(accountService.accrueBatch(payMonth));
    }
}
