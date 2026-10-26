package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.BillQueryDTO;
import com.mis.dto.IdDTO;
import com.mis.service.SettleService;
import com.mis.vo.BillVO;
import com.mis.vo.SettleResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 结算单管理
 */
@RestController
@RequestMapping("/bill")
@RequiredArgsConstructor
public class BillController {

    private final SettleService settleService;

    @GetMapping("/listPage")
    public Result<PageVO<BillVO>> listPage(BillQueryDTO query) {
        return Result.success(settleService.billPage(query));
    }

    @GetMapping("/getDetailById")
    public Result<BillVO> detail(@RequestParam Long id) {
        return Result.success(settleService.billDetail(id));
    }

    @PostMapping("/reverse")
    public Result<SettleResultVO> reverse(@RequestBody IdDTO dto) {
        return Result.success(settleService.reverse(dto.getId()));
    }
}
