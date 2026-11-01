package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.ReportProcessDTO;
import com.mis.dto.ReportQueryDTO;
import com.mis.dto.ReportRewardDTO;
import com.mis.dto.ReportUpsertDTO;
import com.mis.entity.MifReport;
import com.mis.mapper.MifReportMapper;
import com.mis.service.ReportService;
import com.mis.vo.ActionVO;
import com.mis.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final MifReportMapper reportMapper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Override
    public PageVO<ReportVO> page(ReportQueryDTO q) {
        Page<MifReport> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifReport> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifReport::getReportNo, k).or().like(MifReport::getReportedMedinsName, k)
                    .or().like(MifReport::getReporterName, k).or().like(MifReport::getContent, k));
        }
        if (q.getStatus() != null) {
            w.eq(MifReport::getStatus, q.getStatus());
        }
        w.orderByDesc(MifReport::getId);
        Page<MifReport> res = reportMapper.selectPage(page, w);
        List<ReportVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO submit(ReportUpsertDTO dto) {
        MifReport r = new MifReport();
        r.setReportNo("R" + SEQ.incrementAndGet());
        r.setReporterName(dto.getReporterName());
        r.setReporterContact(dto.getReporterContact());
        r.setReportedMedinsCode(dto.getReportedMedinsCode());
        r.setReportedMedinsName(dto.getReportedMedinsName());
        r.setReportedSubject(dto.getReportedSubject());
        r.setProblemType(dto.getProblemType());
        r.setContent(dto.getContent());
        r.setAttachUrl(dto.getAttachUrl());
        r.setStatus(1);
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        reportMapper.insert(r);
        return ActionVO.ok();
    }

    @Override
    public ActionVO process(ReportProcessDTO dto) {
        MifReport r = reportMapper.selectById(dto.getId());
        if (r == null) {
            return ActionVO.fail("举报记录不存在");
        }
        if (r.getStatus() != null && r.getStatus() >= 3) {
            return ActionVO.fail("该举报已办结，不能重复处置");
        }
        r.setStatus(dto.getStatus());
        r.setHandleUser(dto.getHandleUser());
        r.setHandleOpinion(dto.getHandleOpinion());
        r.setHandleTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(r);
        return ActionVO.ok();
    }

    @Override
    public ActionVO reward(ReportRewardDTO dto) {
        MifReport r = reportMapper.selectById(dto.getId());
        if (r == null) {
            return ActionVO.fail("举报记录不存在");
        }
        r.setRewardAmount(dto.getRewardAmount());
        r.setStatus(4);
        r.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(r);
        return ActionVO.ok();
    }

    private ReportVO toVO(MifReport r) {
        ReportVO vo = new ReportVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}
