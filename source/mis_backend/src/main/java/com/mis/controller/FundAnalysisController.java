package com.mis.controller;

import com.mis.common.base.Result;
import com.mis.service.FundAnalysisService;
import com.mis.vo.DimensionStatVO;
import com.mis.vo.FundOverviewVO;
import com.mis.vo.FundTrendVO;
import com.mis.vo.MonthlyAllocStatVO;
import com.mis.vo.OrgRankVO;
import com.mis.vo.SupervisionEffectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 基金运行分析与监管成效分析（统计报表深化）
 */
@RestController
@RequestMapping("/stats/fund")
@RequiredArgsConstructor
public class FundAnalysisController {

    private final FundAnalysisService fundAnalysisService;

    /** 基金运行总览 */
    @GetMapping("/overview")
    public Result<FundOverviewVO> overview(@RequestParam(defaultValue = "30") int rangeDays) {
        return Result.success(fundAnalysisService.overview(rangeDays));
    }

    /** 险种结构 */
    @GetMapping("/insurance-type")
    public Result<List<DimensionStatVO>> byInsuranceType(@RequestParam(defaultValue = "30") int rangeDays) {
        return Result.success(fundAnalysisService.byInsuranceType(rangeDays));
    }

    /** 就诊类型构成 */
    @GetMapping("/encounter-type")
    public Result<List<DimensionStatVO>> byEncounterType(@RequestParam(defaultValue = "30") int rangeDays) {
        return Result.success(fundAnalysisService.byEncounterType(rangeDays));
    }

    /** 支付类别构成 */
    @GetMapping("/pay-class")
    public Result<List<DimensionStatVO>> byPayClass(@RequestParam(defaultValue = "30") int rangeDays) {
        return Result.success(fundAnalysisService.byPayClass(rangeDays));
    }

    /** 项目类别构成 */
    @GetMapping("/category")
    public Result<List<DimensionStatVO>> byCategory(@RequestParam(defaultValue = "30") int rangeDays) {
        return Result.success(fundAnalysisService.byCategory(rangeDays));
    }

    /** 定点机构结算排名 */
    @GetMapping("/org-rank")
    public Result<List<OrgRankVO>> orgRank(@RequestParam(defaultValue = "30") int rangeDays,
                                            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(fundAnalysisService.orgRank(rangeDays, limit));
    }

    /** 基金运行日趋势 */
    @GetMapping("/trend")
    public Result<List<FundTrendVO>> trend(@RequestParam(defaultValue = "30") int rangeDays) {
        return Result.success(fundAnalysisService.trend(rangeDays));
    }

    /** 月度拨付分析 */
    @GetMapping("/monthly-alloc")
    public Result<List<MonthlyAllocStatVO>> monthlyAlloc() {
        return Result.success(fundAnalysisService.monthlyAlloc());
    }

    /** 基金监管成效分析 */
    @GetMapping("/supervision-effect")
    public Result<SupervisionEffectVO> supervisionEffect() {
        return Result.success(fundAnalysisService.supervisionEffect());
    }
}
