package com.mis.service;

import com.mis.vo.DimensionStatVO;
import com.mis.vo.FundOverviewVO;
import com.mis.vo.FundTrendVO;
import com.mis.vo.MonthlyAllocStatVO;
import com.mis.vo.OrgRankVO;
import com.mis.vo.SupervisionEffectVO;

import java.util.List;

public interface FundAnalysisService {

    /** 基金运行总览（核心指标） */
    FundOverviewVO overview(int rangeDays);

    /** 险种结构 */
    List<DimensionStatVO> byInsuranceType(int rangeDays);

    /** 就诊类型构成（门诊/住院） */
    List<DimensionStatVO> byEncounterType(int rangeDays);

    /** 支付类别构成（甲/乙/丙） */
    List<DimensionStatVO> byPayClass(int rangeDays);

    /** 项目类别构成 */
    List<DimensionStatVO> byCategory(int rangeDays);

    /** 定点机构结算排名（TOP N，含违规扣减） */
    List<OrgRankVO> orgRank(int rangeDays, int limit);

    /** 基金运行日趋势 */
    List<FundTrendVO> trend(int rangeDays);

    /** 月度拨付分析 */
    List<MonthlyAllocStatVO> monthlyAlloc();

    /** 基金监管成效分析 */
    SupervisionEffectVO supervisionEffect();
}
