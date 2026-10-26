package com.mis.controller;

import com.mis.common.base.PageResult;
import com.mis.common.base.Result;
import com.mis.dto.DictDataUpsertDTO;
import com.mis.dto.DictTypeUpsertDTO;
import com.mis.dto.DictTypesDTO;
import com.mis.dto.IdDTO;
import com.mis.service.SystemDictService;
import com.mis.vo.DictDataVO;
import com.mis.vo.DictGroupVO;
import com.mis.vo.DictTypeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理（类型 + 数据；兼容 HIS 契约：type/selectList|getById|typeUpsert|deleteById、data/selectList|selectGroup|detail/getById|dataUpsert|deleteById）
 */
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SystemDictController {

    private final SystemDictService dictService;

    // ---------------- 类型 ----------------

    @GetMapping("/typePage")
    public Result<PageResult<DictTypeVO>> typePage(@RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(dictService.typePage(keyword, pageNum, pageSize));
    }

    @GetMapping("/type/selectList")
    public Result<List<DictTypeVO>> typeSelectList() {
        return Result.success(dictService.typeSelectList());
    }

    @GetMapping("/type/getById")
    public Result<DictTypeVO> typeGetById(@RequestParam(required = false) Long typeId,
                                          @RequestParam(required = false) Long id) {
        return Result.success(dictService.typeGetById(typeId, id));
    }

    @PostMapping("/typeUpsert")
    public Result<DictTypeVO> typeUpsert(@Valid @RequestBody DictTypeUpsertDTO dto) {
        return Result.success(dto.getId() == null ? "新增成功" : "修改成功", dictService.typeUpsert(dto));
    }

    @PostMapping("/typeDelete")
    public Result<Void> typeDelete(@RequestBody IdDTO dto) {
        dictService.typeDelete(dto.getId());
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/type/deleteById")
    public Result<Void> typeDeleteParam(@RequestParam(required = false) Long typeId) {
        dictService.typeDelete(typeId);
        return Result.success("删除成功", null);
    }

    // ---------------- 数据 ----------------

    @GetMapping("/dataPage")
    public Result<PageResult<DictDataVO>> dataPage(@RequestParam(required = false) String dictType,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(dictService.dataPage(dictType, keyword, pageNum, pageSize));
    }

    @PostMapping("/data/selectList")
    public Result<List<DictDataVO>> dataSelectList(@RequestBody DictTypesDTO dto) {
        return Result.success(dictService.dataSelectList(dto.getDictTypes()));
    }

    @PostMapping("/data/selectGroup")
    public Result<List<DictGroupVO>> dataSelectGroup(@RequestBody DictTypesDTO dto) {
        return Result.success(dictService.dataSelectGroup(dto.getDictTypes()));
    }

    @GetMapping("/data/detail/getById")
    public Result<DictDataVO> dataDetail(@RequestParam(required = false) Long dataId,
                                         @RequestParam(required = false) Long id) {
        return Result.success(dictService.dataGetById(dataId, id));
    }

    @PostMapping("/dataUpsert")
    public Result<DictDataVO> dataUpsert(@Valid @RequestBody DictDataUpsertDTO dto) {
        return Result.success(dto.getId() == null ? "新增成功" : "修改成功", dictService.dataUpsert(dto));
    }

    @PostMapping("/dataDelete")
    public Result<Void> dataDelete(@RequestBody IdDTO dto) {
        dictService.dataDelete(dto.getId());
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/data/deleteById")
    public Result<Void> dataDeleteParam(@RequestParam(required = false) Long dataId) {
        dictService.dataDelete(dataId);
        return Result.success("删除成功", null);
    }

    @GetMapping("/dataList")
    public Result<List<DictDataVO>> dataList(@RequestParam String dictType) {
        return Result.success(dictService.dataList(dictType));
    }
}
