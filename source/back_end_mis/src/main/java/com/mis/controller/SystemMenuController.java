package com.mis.controller;

import com.mis.common.base.Result;
import com.mis.dto.IdDTO;
import com.mis.dto.MenuUpsertDTO;
import com.mis.service.SystemMenuService;
import com.mis.vo.MenuVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理（树形；兼容 HIS 契约：GET tree/getById，POST menuUpsert，DELETE deleteById）
 */
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SystemMenuController {

    private final SystemMenuService menuService;

    @GetMapping("/tree")
    public Result<List<MenuVO>> tree() {
        return Result.success(menuService.tree());
    }

    @GetMapping("/getById")
    public Result<MenuVO> getById(@RequestParam(required = false) Long menuId,
                                  @RequestParam(required = false) Long id) {
        return Result.success(menuService.getById(menuId, id));
    }

    @PostMapping("/upsert")
    public Result<MenuVO> upsert(@Valid @RequestBody MenuUpsertDTO dto) {
        return Result.success(dto.getId() == null ? "新增成功" : "修改成功", menuService.upsert(dto));
    }

    @PostMapping("/menuUpsert")
    public Result<MenuVO> menuUpsert(@Valid @RequestBody MenuUpsertDTO dto) {
        return Result.success(dto.getId() == null ? "新增成功" : "修改成功", menuService.upsert(dto));
    }

    @PostMapping("/deleteById")
    public Result<Void> deleteById(@RequestBody IdDTO dto) {
        menuService.deleteById(dto.getId());
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/deleteById")
    public Result<Void> deleteByIdParam(@RequestParam(required = false) Long menuId) {
        menuService.deleteById(menuId);
        return Result.success("删除成功", null);
    }
}
