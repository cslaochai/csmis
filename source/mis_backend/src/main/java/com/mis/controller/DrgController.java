package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.DrgCaseOpDTO;
import com.mis.dto.DrgCaseQueryDTO;
import com.mis.dto.DrgGroupQueryDTO;
import com.mis.dto.DrgGroupUpsertDTO;
import com.mis.service.DrgService;
import com.mis.vo.ActionVO;
import com.mis.vo.DrgCaseVO;
import com.mis.vo.DrgGroupVO;
import com.mis.vo.DrgStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * DRG/DIP 支付方式监管
 */
@RestController
@RequestMapping("/drg")
@RequiredArgsConstructor
public class DrgController {

    private final DrgService drgService;

    /* ---------------- 病组 / 病种池 ---------------- */

    @GetMapping("/group/listPage")
    public Result<PageVO<DrgGroupVO>> pageGroup(@Valid DrgGroupQueryDTO q) {
        return Result.success(drgService.pageGroup(q));
    }

    @PostMapping("/group/upsert")
    public Result<ActionVO> upsertGroup(@RequestBody @Valid DrgGroupUpsertDTO dto) {
        return Result.success(drgService.upsertGroup(dto));
    }

    @PostMapping("/group/status")
    public Result<ActionVO> changeGroupStatus(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        Integer status = Integer.valueOf(String.valueOf(body.get("status")));
        return Result.success(drgService.changeGroupStatus(id, status));
    }

    /* ---------------- 病例入组与监管 ---------------- */

    @GetMapping("/case/listPage")
    public Result<PageVO<DrgCaseVO>> pageCase(@Valid DrgCaseQueryDTO q) {
        return Result.success(drgService.pageCase(q));
    }

    /** 批量入组：住院结算单 → 病组匹配 → 偏差/超支判定 */
    @PostMapping("/case/group")
    public Result<ActionVO> groupCases() {
        return Result.success(drgService.groupCases());
    }

    @PostMapping("/case/handle")
    public Result<ActionVO> handleCase(@RequestBody @Valid DrgCaseOpDTO dto) {
        return Result.success(drgService.handleCase(dto));
    }

    @GetMapping("/stats")
    public Result<DrgStatsVO> stats() {
        return Result.success(drgService.stats());
    }
}
