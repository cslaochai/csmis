package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.IdDTO;
import com.mis.dto.MappingMapDTO;
import com.mis.dto.MappingQueryDTO;
import com.mis.service.CatalogService;
import com.mis.vo.ActionVO;
import com.mis.vo.ItemMappingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 院内项目 ↔ 医保目录对照
 */
@RestController
@RequestMapping("/mapping")
@RequiredArgsConstructor
public class MappingController {

    private final CatalogService catalogService;

    @GetMapping("/listPage")
    public Result<PageVO<ItemMappingVO>> listPage(MappingQueryDTO query) {
        return Result.success(catalogService.mappingPage(query));
    }

    @PostMapping("/map")
    public Result<ActionVO> map(@RequestBody MappingMapDTO dto) {
        return Result.success(catalogService.map(dto));
    }

    @PostMapping("/unmap")
    public Result<ActionVO> unmap(@RequestBody IdDTO dto) {
        return Result.success(catalogService.unmap(dto.getId()));
    }

    @PostMapping("/autoMatch")
    public Result<ActionVO> autoMatch() {
        return Result.success(catalogService.autoMatch());
    }
}
