package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.TodoHandleDTO;
import com.mis.dto.TodoQueryDTO;
import com.mis.service.TodoService;
import com.mis.vo.ActionVO;
import com.mis.vo.TodoVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 待办任务中心
 */
@RestController
@RequestMapping("/system/todo")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/listPage")
    public Result<PageVO<TodoVO>> listPage(@Valid TodoQueryDTO q) {
        return Result.success(todoService.page(q));
    }

    @GetMapping("/count")
    public Result<Map<String, Long>> count() {
        return Result.success(todoService.countByStatus());
    }

    @PostMapping("/handle")
    public Result<ActionVO> handle(@RequestBody @Valid TodoHandleDTO dto) {
        return Result.success(todoService.handle(dto));
    }
}
