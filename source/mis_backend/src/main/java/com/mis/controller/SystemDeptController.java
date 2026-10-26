package com.mis.controller;

import com.mis.common.base.Result;
import com.mis.dto.DepartmentUpsertDTO;
import com.mis.service.SystemDeptService;
import com.mis.vo.DepartmentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 经办科室（岗位配置下拉 + HIS 契约 /system/department/**）
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class SystemDeptController {

    private final SystemDeptService deptService;

    @GetMapping("/system/dept/selectList")
    public Result<List<DepartmentVO>> selectList() {
        return Result.success(deptService.selectList());
    }

    @GetMapping("/system/department/tree")
    public Result<List<DepartmentVO>> departmentTree() {
        return Result.success(deptService.tree());
    }

    @GetMapping("/system/department/selectList")
    public Result<List<DepartmentVO>> departmentSelectList() {
        return Result.success(deptService.selectList());
    }

    @GetMapping("/system/department/getById")
    public Result<DepartmentVO> departmentGetById(@RequestParam(required = false) Long deptId,
                                                  @RequestParam(required = false) Long id) {
        return Result.success(deptService.getById(deptId, id));
    }

    @PostMapping("/system/department/departmentUpsert")
    public Result<DepartmentVO> departmentUpsert(@RequestBody @Valid DepartmentUpsertDTO dto) {
        DepartmentVO dept = deptService.upsert(dto);
        return Result.success("保存成功", dept);
    }

    @DeleteMapping("/system/department/deleteById")
    public Result<Void> departmentDeleteById(@RequestParam(required = false) Long deptId) {
        deptService.deleteById(deptId);
        return Result.success("删除成功", null);
    }
}
