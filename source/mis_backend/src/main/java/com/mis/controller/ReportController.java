package com.mis.controller;

import com.mis.common.base.PageVO;
import com.mis.common.base.Result;
import com.mis.dto.ReportProcessDTO;
import com.mis.dto.ReportQueryDTO;
import com.mis.dto.ReportRewardDTO;
import com.mis.dto.ReportUpsertDTO;
import com.mis.service.ReportService;
import com.mis.vo.ActionVO;
import com.mis.vo.ReportVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supervise/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/listPage")
    public Result<PageVO<ReportVO>> listPage(@Valid ReportQueryDTO q) {
        return Result.success(reportService.page(q));
    }

    @PostMapping("/submit")
    public Result<ActionVO> submit(@RequestBody ReportUpsertDTO dto) {
        return Result.success(reportService.submit(dto));
    }

    @PostMapping("/process")
    public Result<ActionVO> process(@RequestBody @Valid ReportProcessDTO dto) {
        return Result.success(reportService.process(dto));
    }

    @PostMapping("/reward")
    public Result<ActionVO> reward(@RequestBody @Valid ReportRewardDTO dto) {
        return Result.success(reportService.reward(dto));
    }
}
