package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.OperLogQueryDTO;
import com.mis.entity.SysOperLog;
import com.mis.mapper.SysOperLogMapper;
import com.mis.service.OperLogService;
import com.mis.vo.OperLogStatVO;
import com.mis.vo.OperLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {

    private final SysOperLogMapper operLogMapper;

    @Override
    public PageVO<OperLogVO> page(OperLogQueryDTO q) {
        LambdaQueryWrapper<SysOperLog> mw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getOperType())) {
            mw.eq(SysOperLog::getOperType, q.getOperType());
        }
        if (StringUtils.hasText(q.getOperModule())) {
            mw.eq(SysOperLog::getOperModule, q.getOperModule());
        }
        if (StringUtils.hasText(q.getOperUser())) {
            mw.like(SysOperLog::getOperUser, q.getOperUser());
        }
        if (StringUtils.hasText(q.getTargetType())) {
            mw.eq(SysOperLog::getTargetType, q.getTargetType());
        }
        if (StringUtils.hasText(q.getTargetNo())) {
            mw.like(SysOperLog::getTargetNo, q.getTargetNo());
        }
        if (q.getOperResult() != null) {
            mw.eq(SysOperLog::getOperResult, q.getOperResult());
        }
        if (StringUtils.hasText(q.getRequestUri())) {
            mw.like(SysOperLog::getRequestUri, q.getRequestUri());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            mw.and(w -> w.like(SysOperLog::getOperDesc, q.getKeyword())
                    .or().like(SysOperLog::getTargetNo, q.getKeyword())
                    .or().like(SysOperLog::getRequestUri, q.getKeyword()));
        }
        if (StringUtils.hasText(q.getBeginDate())) {
            mw.ge(SysOperLog::getCreateTime, q.getBeginDate() + " 00:00:00");
        }
        if (StringUtils.hasText(q.getEndDate())) {
            mw.le(SysOperLog::getCreateTime, q.getEndDate() + " 23:59:59");
        }
        mw.orderByDesc(SysOperLog::getCreateTime).orderByDesc(SysOperLog::getId);

        IPage<SysOperLog> pg = operLogMapper.selectPage(
                new Page<>(q.getPage(), q.getSize()), mw);
        List<OperLogVO> list = new ArrayList<>();
        for (SysOperLog e : pg.getRecords()) {
            list.add(toVO(e));
        }
        return PageVO.of(list, pg.getTotal());
    }

    /**
     * 异步落库：写日志失败绝不能影响主业务流程（医保系统审计要求「记录但不阻断」）
     */
    @Async("logTaskExecutor")
    @Override
    public void save(OperLogVO vo) {
        try {
            SysOperLog e = new SysOperLog();
            BeanUtils.copyProperties(vo, e);
            e.setId(null);
            operLogMapper.insert(e);
        } catch (Exception ex) {
            log.warn("[操作日志] 写入失败（不影响主流程）：{}", ex.getMessage());
        }
    }

    @Override
    public OperLogStatVO stat(Integer days) {
        int d = days == null || days <= 0 ? 30 : Math.min(days, 365);
        LocalDateTime from = LocalDate.now().minusDays(d - 1).atStartOfDay();
        OperLogStatVO vo = new OperLogStatVO();
        vo.setDays(d);

        List<SysOperLog> all = operLogMapper.selectList(new LambdaQueryWrapper<SysOperLog>()
                .ge(SysOperLog::getCreateTime, from));
        vo.setTotal((long) all.size());
        long ok = all.stream().filter(e -> e.getOperResult() == null || e.getOperResult() == 1).count();
        vo.setSuccessCount(ok);
        vo.setFailCount(all.size() - ok);
        vo.setUserCount(all.stream().map(SysOperLog::getOperUser).filter(StringUtils::hasText).distinct().count());
        vo.setAvgCostMs(all.stream().map(SysOperLog::getCostMs).filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue).sum() / Math.max(1, all.size()));

        Map<String, Long> typeMap = new HashMap<>();
        Map<String, Long> moduleMap = new HashMap<>();
        Map<String, long[]> dayMap = new HashMap<>();
        for (SysOperLog e : all) {
            typeMap.merge(nullToEmpty(e.getOperType()), 1L, Long::sum);
            moduleMap.merge(nullToEmpty(e.getOperModule()), 1L, Long::sum);
            String day = e.getCreateTime() == null ? "" : e.getCreateTime().toLocalDate().toString();
            long[] arr = dayMap.computeIfAbsent(day, k -> new long[2]);
            arr[0]++;
            if (e.getOperResult() != null && e.getOperResult() == 0) {
                arr[1]++;
            }
        }
        vo.setByType(toDimList(typeMap));
        vo.setByModule(toDimList(moduleMap));

        List<OperLogStatVO.DayCount> days_ = new ArrayList<>();
        for (int i = 0; i < d; i++) {
            String dk = LocalDate.now().minusDays(d - 1 - i).toString();
            long[] arr = dayMap.get(dk);
            OperLogStatVO.DayCount dc = new OperLogStatVO.DayCount();
            dc.setDay(dk);
            dc.setCount(arr == null ? 0L : arr[0]);
            dc.setFailCount(arr == null ? 0L : arr[1]);
            days_.add(dc);
        }
        vo.setByDay(days_);
        return vo;
    }

    @Override
    public int clean(Integer keepDays) {
        int k = keepDays == null || keepDays <= 0 ? 180 : Math.min(keepDays, 3650);
        return operLogMapper.delete(new LambdaQueryWrapper<SysOperLog>()
                .lt(SysOperLog::getCreateTime, LocalDateTime.now().minusDays(k)));
    }

    private List<OperLogStatVO.DimCount> toDimList(Map<String, Long> map) {
        List<OperLogStatVO.DimCount> list = new ArrayList<>();
        map.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> {
                    OperLogStatVO.DimCount dc = new OperLogStatVO.DimCount();
                    dc.setName(e.getKey());
                    dc.setCount(e.getValue());
                    list.add(dc);
                });
        return list;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private OperLogVO toVO(SysOperLog e) {
        OperLogVO vo = new OperLogVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
