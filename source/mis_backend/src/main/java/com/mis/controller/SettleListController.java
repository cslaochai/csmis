package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.SettleListQueryDTO;
import com.mis.service.SettleListService;
import com.mis.vo.SettleListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算清单（报送主表）
 */
@RestController
@RequestMapping("/settle/list")
@RequiredArgsConstructor
public class SettleListController {

    private final SettleListService settleListService;

    @GetMapping("/listPage")
    public Result<PageVO<SettleListVO>> listPage(@Valid SettleListQueryDTO q) {
        return Result.success(settleListService.page(q));
    }

    @GetMapping("/detail/{id}")
    public Result<SettleListVO> detail(@PathVariable Long id) {
        return Result.success(settleListService.detail(id));
    }
}
