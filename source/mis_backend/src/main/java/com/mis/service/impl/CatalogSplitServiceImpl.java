package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mis.common.base.PageVO;
import com.mis.dto.CatalogSplitQueryDTO;
import com.mis.entity.MisCatalog;
import com.mis.entity.MisDrugCatalog;
import com.mis.entity.MisMaterialCatalog;
import com.mis.entity.MisTreatCatalog;
import com.mis.mapper.MisCatalogMapper;
import com.mis.mapper.MisDrugCatalogMapper;
import com.mis.mapper.MisMaterialCatalogMapper;
import com.mis.mapper.MisTreatCatalogMapper;
import com.mis.service.CatalogSplitService;
import com.mis.vo.CatalogSplitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 目录分表服务实现
 */
@Service
@RequiredArgsConstructor
public class CatalogSplitServiceImpl implements CatalogSplitService {

    private final MisDrugCatalogMapper drugMapper;
    private final MisTreatCatalogMapper treatMapper;
    private final MisMaterialCatalogMapper materialMapper;
    private final MisCatalogMapper catalogMapper;

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Override
    public PageVO<CatalogSplitVO> page(CatalogSplitQueryDTO q) {
        String type = q.getCatalogType();
        List<CatalogSplitVO> all = new ArrayList<>();
        if (!StringUtils.hasText(type) || "drug".equals(type)) {
            all.addAll(listDrugs(null));
        }
        if (!StringUtils.hasText(type) || "treat".equals(type)) {
            all.addAll(listTreats(q));
        }
        if (!StringUtils.hasText(type) || "material".equals(type)) {
            all.addAll(listMaterials(q));
        }
        // 关键词过滤（内存）
        List<CatalogSplitVO> filtered = all;
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword().trim();
            filtered = all.stream()
                    .filter(v -> contains(v.getName(), k) || contains(v.getCode(), k)
                            || contains(v.getGenericName(), k) || contains(v.getYbCode(), k))
                    .toList();
        }
        long total = filtered.size();
        int from = (int) Math.min(total, (q.getPage() - 1) * q.getSize());
        int to = (int) Math.min(total, from + q.getSize());
        return PageVO.of(filtered.subList(from, to), total);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> m = new HashMap<>();
        m.put("drug", drugMapper.selectCount(null));
        m.put("treat", treatMapper.selectCount(null));
        m.put("material", materialMapper.selectCount(null));
        m.put("catalog", catalogMapper.selectCount(null));
        m.put("drugEnabled", drugMapper.selectCount(
                new LambdaQueryWrapper<MisDrugCatalog>().eq(MisDrugCatalog::getStatus, 1)));
        m.put("treatEnabled", treatMapper.selectCount(
                new LambdaQueryWrapper<MisTreatCatalog>().eq(MisTreatCatalog::getStatus, 1)));
        m.put("materialEnabled", materialMapper.selectCount(
                new LambdaQueryWrapper<MisMaterialCatalog>().eq(MisMaterialCatalog::getStatus, 1)));
        return m;
    }

    @Override
    public List<CatalogSplitVO> listDrugs(String keyword) {
        List<MisDrugCatalog> rows = drugMapper.selectList(new LambdaQueryWrapper<MisDrugCatalog>()
                .orderByAsc(MisDrugCatalog::getDrugCode));
        List<CatalogSplitVO> out = new ArrayList<>();
        for (MisDrugCatalog d : rows) {
            CatalogSplitVO vo = new CatalogSplitVO();
            vo.setId(d.getId());
            vo.setCatalogType("drug");
            vo.setCatalogTypeName("药品目录");
            vo.setCode(d.getDrugCode());
            vo.setName(d.getDrugName());
            vo.setGenericName(d.getGenericName());
            vo.setDosageForm(d.getDosageForm());
            vo.setStrength(d.getStrength());
            vo.setManufacturerCode(d.getManufacturerCode());
            vo.setYbCode(d.getYbCode());
            vo.setPayClass(d.getPayClass());
            vo.setSelfRatio(d.getSelfRatio());
            vo.setInsRatio(d.getInsRatio());
            vo.setLimitPrice(d.getLimitPrice());
            vo.setUnit(d.getUnit());
            vo.setStatus(d.getStatus());
            vo.setCreateTime(d.getCreateTime());
            out.add(vo);
        }
        return out;
    }

    private List<CatalogSplitVO> listTreats(CatalogSplitQueryDTO q) {
        List<MisTreatCatalog> rows = treatMapper.selectList(new LambdaQueryWrapper<MisTreatCatalog>()
                .orderByAsc(MisTreatCatalog::getTreatCode));
        List<CatalogSplitVO> out = new ArrayList<>();
        for (MisTreatCatalog t : rows) {
            CatalogSplitVO vo = new CatalogSplitVO();
            vo.setId(t.getId());
            vo.setCatalogType("treat");
            vo.setCatalogTypeName("诊疗目录");
            vo.setCode(t.getTreatCode());
            vo.setName(t.getTreatName());
            vo.setTreatLevel(t.getTreatLevel());
            vo.setYbCode(t.getYbCode());
            vo.setPayClass(t.getPayClass());
            vo.setInsRatio(t.getInsRatio());
            vo.setLimitPrice(t.getLimitPrice());
            vo.setUnit(t.getUnit());
            vo.setStatus(t.getStatus());
            vo.setCreateTime(t.getCreateTime());
            out.add(vo);
        }
        return out;
    }

    private List<CatalogSplitVO> listMaterials(CatalogSplitQueryDTO q) {
        List<MisMaterialCatalog> rows = materialMapper.selectList(new LambdaQueryWrapper<MisMaterialCatalog>()
                .orderByAsc(MisMaterialCatalog::getMaterialCode));
        List<CatalogSplitVO> out = new ArrayList<>();
        for (MisMaterialCatalog m : rows) {
            CatalogSplitVO vo = new CatalogSplitVO();
            vo.setId(m.getId());
            vo.setCatalogType("material");
            vo.setCatalogTypeName("耗材目录");
            vo.setCode(m.getMaterialCode());
            vo.setName(m.getMaterialName());
            vo.setMaterialType(m.getMaterialType());
            vo.setSpec(m.getSpec());
            vo.setYbCode(m.getYbCode());
            vo.setPayClass(m.getPayClass());
            vo.setLimitPrice(m.getLimitPrice());
            vo.setUnit(m.getUnit());
            vo.setStatus(m.getStatus());
            vo.setCreateTime(m.getCreateTime());
            out.add(vo);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // 权威数据源：限价 / 支付类别 / 支付比例
    // ------------------------------------------------------------------

    @Override
    public BigDecimal limitPriceOf(String ybCode) {
        if (!StringUtils.hasText(ybCode)) {
            return null;
        }
        MisDrugCatalog d = drugMapper.selectOne(new LambdaQueryWrapper<MisDrugCatalog>()
                .eq(MisDrugCatalog::getYbCode, ybCode).last("limit 1"));
        if (d != null && d.getLimitPrice() != null) {
            return d.getLimitPrice();
        }
        MisTreatCatalog t = treatMapper.selectOne(new LambdaQueryWrapper<MisTreatCatalog>()
                .eq(MisTreatCatalog::getYbCode, ybCode).last("limit 1"));
        if (t != null && t.getLimitPrice() != null) {
            return t.getLimitPrice();
        }
        MisMaterialCatalog m = materialMapper.selectOne(new LambdaQueryWrapper<MisMaterialCatalog>()
                .eq(MisMaterialCatalog::getYbCode, ybCode).last("limit 1"));
        if (m != null && m.getLimitPrice() != null) {
            return m.getLimitPrice();
        }
        // 回落聚合目录
        MisCatalog c = catalogMapper.selectOne(new LambdaQueryWrapper<MisCatalog>()
                .eq(MisCatalog::getYbCode, ybCode).last("limit 1"));
        return c == null ? null : c.getLimitPrice();
    }

    @Override
    public String payClassOf(String ybCode) {
        if (!StringUtils.hasText(ybCode)) {
            return null;
        }
        MisDrugCatalog d = drugMapper.selectOne(new LambdaQueryWrapper<MisDrugCatalog>()
                .eq(MisDrugCatalog::getYbCode, ybCode).last("limit 1"));
        if (d != null && StringUtils.hasText(d.getPayClass())) {
            return d.getPayClass();
        }
        MisTreatCatalog t = treatMapper.selectOne(new LambdaQueryWrapper<MisTreatCatalog>()
                .eq(MisTreatCatalog::getYbCode, ybCode).last("limit 1"));
        if (t != null && StringUtils.hasText(t.getPayClass())) {
            return t.getPayClass();
        }
        MisMaterialCatalog m = materialMapper.selectOne(new LambdaQueryWrapper<MisMaterialCatalog>()
                .eq(MisMaterialCatalog::getYbCode, ybCode).last("limit 1"));
        if (m != null && StringUtils.hasText(m.getPayClass())) {
            return m.getPayClass();
        }
        MisCatalog c = catalogMapper.selectOne(new LambdaQueryWrapper<MisCatalog>()
                .eq(MisCatalog::getYbCode, ybCode).last("limit 1"));
        return c == null ? null : c.getPayClass();
    }

    @Override
    public BigDecimal insRatioOf(String ybCode) {
        if (!StringUtils.hasText(ybCode)) {
            return null;
        }
        MisDrugCatalog d = drugMapper.selectOne(new LambdaQueryWrapper<MisDrugCatalog>()
                .eq(MisDrugCatalog::getYbCode, ybCode).last("limit 1"));
        if (d != null && d.getInsRatio() != null) {
            return d.getInsRatio();
        }
        MisTreatCatalog t = treatMapper.selectOne(new LambdaQueryWrapper<MisTreatCatalog>()
                .eq(MisTreatCatalog::getYbCode, ybCode).last("limit 1"));
        if (t != null && t.getInsRatio() != null) {
            return t.getInsRatio();
        }
        MisCatalog c = catalogMapper.selectOne(new LambdaQueryWrapper<MisCatalog>()
                .eq(MisCatalog::getYbCode, ybCode).last("limit 1"));
        return c == null ? null : c.getInsuranceRatio();
    }

    @Override
    public Map<String, BigDecimal> loadLimitPrices() {
        Map<String, BigDecimal> m = new HashMap<>();
        for (MisDrugCatalog d : drugMapper.selectList(null)) {
            if (StringUtils.hasText(d.getYbCode()) && d.getLimitPrice() != null) {
                m.put(d.getYbCode(), d.getLimitPrice());
            }
        }
        for (MisTreatCatalog t : treatMapper.selectList(null)) {
            if (StringUtils.hasText(t.getYbCode()) && t.getLimitPrice() != null) {
                m.putIfAbsent(t.getYbCode(), t.getLimitPrice());
            }
        }
        for (MisMaterialCatalog x : materialMapper.selectList(null)) {
            if (StringUtils.hasText(x.getYbCode()) && x.getLimitPrice() != null) {
                m.putIfAbsent(x.getYbCode(), x.getLimitPrice());
            }
        }
        // 聚合目录兜底
        for (MisCatalog c : catalogMapper.selectList(null)) {
            if (StringUtils.hasText(c.getYbCode()) && c.getLimitPrice() != null) {
                m.putIfAbsent(c.getYbCode(), c.getLimitPrice());
            }
        }
        return m;
    }

    @Override
    public Map<String, String> loadPayClasses() {
        Map<String, String> m = new HashMap<>();
        for (MisDrugCatalog d : drugMapper.selectList(null)) {
            if (StringUtils.hasText(d.getYbCode()) && StringUtils.hasText(d.getPayClass())) {
                m.put(d.getYbCode(), d.getPayClass());
            }
        }
        for (MisTreatCatalog t : treatMapper.selectList(null)) {
            if (StringUtils.hasText(t.getYbCode()) && StringUtils.hasText(t.getPayClass())) {
                m.putIfAbsent(t.getYbCode(), t.getPayClass());
            }
        }
        for (MisMaterialCatalog x : materialMapper.selectList(null)) {
            if (StringUtils.hasText(x.getYbCode()) && StringUtils.hasText(x.getPayClass())) {
                m.putIfAbsent(x.getYbCode(), x.getPayClass());
            }
        }
        for (MisCatalog c : catalogMapper.selectList(null)) {
            if (StringUtils.hasText(c.getYbCode()) && StringUtils.hasText(c.getPayClass())) {
                m.putIfAbsent(c.getYbCode(), c.getPayClass());
            }
        }
        return m;
    }

    @Override
    public Map<String, BigDecimal> loadInsRatios() {
        Map<String, BigDecimal> m = new HashMap<>();
        for (MisDrugCatalog d : drugMapper.selectList(null)) {
            if (StringUtils.hasText(d.getYbCode()) && d.getInsRatio() != null) {
                m.put(d.getYbCode(), d.getInsRatio());
            }
        }
        for (MisTreatCatalog t : treatMapper.selectList(null)) {
            if (StringUtils.hasText(t.getYbCode()) && t.getInsRatio() != null) {
                m.putIfAbsent(t.getYbCode(), t.getInsRatio());
            }
        }
        for (MisCatalog c : catalogMapper.selectList(null)) {
            if (StringUtils.hasText(c.getYbCode()) && c.getInsuranceRatio() != null) {
                m.putIfAbsent(c.getYbCode(), c.getInsuranceRatio());
            }
        }
        return m;
    }

    private boolean contains(String v, String k) {
        return v != null && v.toLowerCase().contains(k.toLowerCase());
    }
}
