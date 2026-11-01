package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.CatalogQueryDTO;
import com.mis.dto.MappingMapDTO;
import com.mis.dto.MappingQueryDTO;
import com.mis.entity.MisCatalog;
import com.mis.entity.MisItemMapping;
import com.mis.mapper.MisCatalogMapper;
import com.mis.mapper.MisItemMappingMapper;
import com.mis.service.CatalogService;
import com.mis.vo.ActionVO;
import com.mis.vo.CatalogStatsVO;
import com.mis.vo.CatalogVO;
import com.mis.vo.ItemMappingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final MisCatalogMapper catalogMapper;
    private final MisItemMappingMapper mappingMapper;

    private static final List<String> CATEGORIES =
            List.of("西药", "中成药", "中草药", "诊疗项目", "医用耗材");

    @Override
    public PageVO<CatalogVO> catalogPage(CatalogQueryDTO q) {
        Page<MisCatalog> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisCatalog> w = new LambdaQueryWrapper<>();
        if (hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(MisCatalog::getName, kw).or().like(MisCatalog::getYbCode, kw));
        }
        if (hasText(q.getCategory())) {
            w.eq(MisCatalog::getCategory, q.getCategory());
        }
        if (hasText(q.getPayClass())) {
            w.eq(MisCatalog::getPayClass, q.getPayClass());
        }
        w.orderByAsc(MisCatalog::getId);
        Page<MisCatalog> res = catalogMapper.selectPage(page, w);
        List<CatalogVO> vos = res.getRecords().stream().map(this::toCatalogVO).collect(java.util.stream.Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public CatalogStatsVO catalogStats() {
        List<MisCatalog> all = catalogMapper.selectList(null);
        Map<String, Long> byCat = new LinkedHashMap<>();
        CATEGORIES.forEach(c -> byCat.put(c, 0L));
        Map<String, Long> byClass = new LinkedHashMap<>();
        byClass.put("甲", 0L);
        byClass.put("乙", 0L);
        byClass.put("丙", 0L);
        long disabled = 0;
        for (MisCatalog c : all) {
            byCat.merge(c.getCategory(), 1L, Long::sum);
            if (c.getPayClass() != null) {
                byClass.merge(c.getPayClass(), 1L, Long::sum);
            }
            if (c.getStatus() != null && c.getStatus() == 0) {
                disabled++;
            }
        }
        CatalogStatsVO vo = new CatalogStatsVO();
        vo.setByCat(byCat);
        vo.setByClass(byClass);
        vo.setTotal(all.size());
        vo.setDisabled(disabled);
        return vo;
    }

    @Override
    public PageVO<ItemMappingVO> mappingPage(MappingQueryDTO q) {
        Page<MisItemMapping> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisItemMapping> w = new LambdaQueryWrapper<>();
        if (hasText(q.getKeyword())) {
            w.like(MisItemMapping::getHospItemName, q.getKeyword().trim());
        }
        if (q.getMapped() != null) {
            w.eq(MisItemMapping::getMapped, q.getMapped());
        }
        w.orderByAsc(MisItemMapping::getId);
        Page<MisItemMapping> res = mappingMapper.selectPage(page, w);
        List<ItemMappingVO> vos = res.getRecords().stream().map(this::toItemMappingVO)
                .collect(java.util.stream.Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO map(MappingMapDTO dto) {
        if (dto.getId() == null) {
            return ActionVO.fail("缺少对照记录id");
        }
        MisItemMapping m = mappingMapper.selectById(dto.getId());
        if (m == null) {
            return ActionVO.fail("对照记录不存在");
        }
        m.setYbCode(dto.getYbCode());
        m.setYbName(dto.getYbName());
        m.setPayClass(dto.getPayClass());
        m.setMapped(true);
        m.setUpdatedAt(LocalDateTime.now());
        mappingMapper.updateById(m);
        return ActionVO.ok();
    }

    @Override
    public ActionVO unmap(Long id) {
        MisItemMapping m = mappingMapper.selectById(id);
        if (m == null) {
            return ActionVO.fail("对照记录不存在");
        }
        m.setYbCode("");
        m.setYbName("");
        m.setPayClass("");
        m.setMapped(false);
        m.setUpdatedAt(null);
        mappingMapper.updateById(m);
        return ActionVO.ok();
    }

    @Override
    public ActionVO autoMatch() {
        List<MisCatalog> catalog = catalogMapper.selectList(null);
        List<MisItemMapping> mappings = mappingMapper.selectList(null);
        int n = 0;
        for (MisItemMapping m : mappings) {
            if (Boolean.TRUE.equals(m.getMapped())) {
                continue;
            }
            String hospHead = m.getHospItemName() == null ? "" : m.getHospItemName();
            String head = hospHead.length() >= 2 ? hospHead.substring(0, 2) : hospHead;
            MisCatalog hit = null;
            for (MisCatalog c : catalog) {
                String name = c.getName() == null ? "" : c.getName();
                String cHead = name.length() >= 2 ? name.substring(0, 2) : name;
                if (name.contains(head) || hospHead.contains(cHead)) {
                    hit = c;
                    break;
                }
            }
            if (hit != null) {
                m.setMapped(true);
                m.setYbCode(hit.getYbCode());
                m.setYbName(hit.getName());
                m.setPayClass(hit.getPayClass());
                m.setUpdatedAt(LocalDateTime.now());
                mappingMapper.updateById(m);
                n++;
            }
        }
        return ActionVO.matched(n);
    }

    private CatalogVO toCatalogVO(MisCatalog c) {
        CatalogVO vo = new CatalogVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }

    private ItemMappingVO toItemMappingVO(MisItemMapping m) {
        ItemMappingVO vo = new ItemMappingVO();
        BeanUtils.copyProperties(m, vo);
        return vo;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
