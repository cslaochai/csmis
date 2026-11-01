package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.KnowledgeQueryDTO;
import com.mis.dto.KnowledgeUpsertDTO;
import com.mis.service.KnowledgeService;
import com.mis.vo.ActionVO;
import com.mis.vo.KnowledgeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 基金监管 - 知识库（两库之一）
 */
@RestController
@RequestMapping("/supervise/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/listPage")
    public Result<PageVO<KnowledgeVO>> listPage(KnowledgeQueryDTO query) {
        return Result.success(knowledgeService.page(query));
    }

    @PostMapping("/upsert")
    public Result<ActionVO> upsert(@RequestBody @Valid KnowledgeUpsertDTO dto) {
        return Result.success(knowledgeService.upsert(dto));
    }

    @PostMapping("/status")
    public Result<ActionVO> status(@RequestParam Long id, @RequestParam Integer status) {
        return Result.success(knowledgeService.changeStatus(id, status));
    }

    @GetMapping("/detail")
    public Result<KnowledgeVO> detail(@RequestParam Long id) {
        return Result.success(knowledgeService.detail(id));
    }
}
