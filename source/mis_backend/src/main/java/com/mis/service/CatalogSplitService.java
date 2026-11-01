package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.CatalogSplitQueryDTO;
import com.mis.vo.CatalogSplitVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 目录分表服务（药品 / 诊疗 / 耗材）
 * <p>医保目录按批次发布，真实系统会把药品、诊疗项目、医用耗材分成三张独立的目录表
 * （字段结构与业务口径各不相同），而不是混在一张表里用 category 区分。
 * <p>本服务提供跨分表的统一查询能力，并把分表作为限价与支付类别的权威来源：
 * 结算、事前提醒、清单质控都从这里取限价/支付类别。
 */
public interface CatalogSplitService {

    /** 统一分页查询（catalogType: drug / treat / material，为空查全部） */
    PageVO<CatalogSplitVO> page(CatalogSplitQueryDTO q);

    /** 目录统计（各分表条数与启用数） */
    java.util.Map<String, Object> stats();

    /**
     * 按医保编码查限价（分表优先，找不到回落聚合目录）。
     *
     * @param ybCode 医保编码
     * @return 限价，无则返回 null
     */
    BigDecimal limitPriceOf(String ybCode);

    /**
     * 按医保编码查支付类别（甲/乙/丙）。
     */
    String payClassOf(String ybCode);

    /**
     * 按医保编码查支付比例。
     *
     * @return 支付比例，无则返回 null
     */
    BigDecimal insRatioOf(String ybCode);

    /**
     * 批量载入限价/支付类别映射（供引擎一次性加载，避免逐条查库）。
     *
     * @return key=医保编码，value=限价
     */
    java.util.Map<String, BigDecimal> loadLimitPrices();

    /** 批量载入支付类别映射 */
    java.util.Map<String, String> loadPayClasses();

    /** 批量载入支付比例映射 */
    java.util.Map<String, BigDecimal> loadInsRatios();

    /** 药品分表明细（含通用名/剂型/厂家），用于监管端展示 */
    List<CatalogSplitVO> listDrugs(String keyword);
}
