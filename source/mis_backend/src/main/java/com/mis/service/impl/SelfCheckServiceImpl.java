package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.SelfCheckActionDTO;
import com.mis.dto.SelfCheckQueryDTO;
import com.mis.dto.SelfCheckUpsertDTO;
import com.mis.entity.MifSelfCheck;
import com.mis.mapper.MifSelfCheckMapper;
import com.mis.service.SelfCheckService;
import com.mis.vo.ActionVO;
import com.mis.vo.SelfCheckVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 机构自查自纠：下发 → 自查 → 上报 → 整改 → 复核办结
 */
@Service
@RequiredArgsConstructor
public class SelfCheckServiceImpl implements SelfCheckService {

    private final MifSelfCheckMapper mapper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Override
    public PageVO<SelfCheckVO> page(SelfCheckQueryDTO q) {
        Page<MifSelfCheck> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifSelfCheck> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifSelfCheck::getTaskNo, k).or().like(MifSelfCheck::getTaskName, k)
                    .or().like(MifSelfCheck::getMedinsName, k));
        }
        if (StringUtils.hasText(q.getMedinsCode())) {
            w.eq(MifSelfCheck::getMedinsCode, q.getMedinsCode());
        }
        if (q.getStatus() != null) {
            w.eq(MifSelfCheck::getStatus, q.getStatus());
        }
        w.orderByDesc(MifSelfCheck::getId);
        Page<MifSelfCheck> res = mapper.selectPage(page, w);
        List<SelfCheckVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsert(SelfCheckUpsertDTO dto) {
        MifSelfCheck c;
        if (dto.getId() == null) {
            c = new MifSelfCheck();
            c.setTaskNo("ZC" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%04d", SEQ.incrementAndGet()));
            c.setStartDate(LocalDate.now());
            c.setStatus(1);
        } else {
            c = mapper.selectById(dto.getId());
            if (c == null) {
                return ActionVO.fail("自查自纠任务不存在");
            }
        }
        c.setMedinsCode(dto.getMedinsCode());
        c.setMedinsName(dto.getMedinsName());
        c.setTaskName(dto.getTaskName());
        c.setCheckScope(dto.getCheckScope());
        c.setDeadline(dto.getDeadline());
        c.setRemark(dto.getRemark());
        c.setUpdateTime(LocalDateTime.now());
        if (dto.getId() == null) {
            c.setCreateTime(LocalDateTime.now());
            mapper.insert(c);
        } else {
            mapper.updateById(c);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO start(Long id) {
        MifSelfCheck c = mapper.selectById(id);
        if (c == null) {
            return ActionVO.fail("自查自纠任务不存在");
        }
        if (c.getStatus() != null && c.getStatus() != 1) {
            return ActionVO.fail("仅「待自查」状态的任务可开始自查");
        }
        c.setStatus(2);
        c.setUpdateTime(LocalDateTime.now());
        mapper.updateById(c);
        return ActionVO.ok();
    }

    @Override
    public ActionVO submit(SelfCheckActionDTO dto) {
        MifSelfCheck c = mapper.selectById(dto.getId());
        if (c == null) {
            return ActionVO.fail("自查自纠任务不存在");
        }
        if (c.getStatus() == null || (c.getStatus() != 2 && c.getStatus() != 4)) {
            return ActionVO.fail("仅「自查中」或「整改中」的任务可上报");
        }
        c.setSubmitContent(dto.getContent());
        c.setSubmitTime(LocalDateTime.now());
        if (c.getStatus() == 4) {
            // 整改后再次上报 → 已整改，等待复核
            c.setRectifyContent(dto.getContent());
            c.setRectifyTime(LocalDateTime.now());
            c.setStatus(4);
        } else {
            c.setStatus(3);
        }
        c.setUpdateTime(LocalDateTime.now());
        mapper.updateById(c);
        return ActionVO.ok();
    }

    @Override
    public ActionVO review(SelfCheckActionDTO dto) {
        MifSelfCheck c = mapper.selectById(dto.getId());
        if (c == null) {
            return ActionVO.fail("自查自纠任务不存在");
        }
        if (c.getStatus() == null || (c.getStatus() != 3 && c.getStatus() != 4)) {
            return ActionVO.fail("仅「已上报」或「已整改」的任务可复核");
        }
        boolean passed = Boolean.TRUE.equals(dto.getPassed());
        c.setReviewer(UserContext.username());
        c.setReviewOpinion(dto.getOpinion());
        c.setReviewTime(LocalDateTime.now());
        c.setStatus(passed ? 5 : 4);
        c.setUpdateTime(LocalDateTime.now());
        mapper.updateById(c);
        return ActionVO.ok();
    }

    private SelfCheckVO toVO(MifSelfCheck c) {
        SelfCheckVO vo = new SelfCheckVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
