package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.CatalogSplitQueryDTO;
import com.mis.service.CatalogSplitService;
import com.mis.vo.CatalogSplitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 目录分表（药品 / 诊疗 / 耗材）
 */
@RestController
@RequestMapping("/settle/catalog")
@RequiredArgsConstructor
public class CatalogSplitController {

    private final CatalogSplitService catalogSplitService;

    @GetMapping("/listPage")
    public Result<PageVO<CatalogSplitVO>> listPage(@Valid CatalogSplitQueryDTO q) {
        return Result.success(catalogSplitService.page(q));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(catalogSplitService.stats());
    }
}
