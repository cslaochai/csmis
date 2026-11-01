package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.FilingQueryDTO;
import com.mis.dto.FilingUpsertDTO;
import com.mis.entity.MisFiling;
import com.mis.mapper.MisFilingMapper;
import com.mis.service.FilingService;
import com.mis.vo.ActionVO;
import com.mis.vo.FilingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilingServiceImpl implements FilingService {

    private final MisFilingMapper filingMapper;

    @Override
    public PageVO<FilingVO> filingPage(FilingQueryDTO q) {
        Page<MisFiling> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisFiling> w = new LambdaQueryWrapper<>();
        if (hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(MisFiling::getName, kw)
                    .or().like(MisFiling::getInsuranceArea, kw)
                    .or().like(MisFiling::getFromArea, kw)
                    .or().like(MisFiling::getHospital, kw));
        }
        if (hasText(q.getStatus())) {
            w.eq(MisFiling::getStatus, q.getStatus());
        }
        w.orderByDesc(MisFiling::getId);
        Page<MisFiling> res = filingMapper.selectPage(page, w);
        List<FilingVO> vos = res.getRecords().stream().map(f -> {
            FilingVO vo = new FilingVO();
            BeanUtils.copyProperties(f, vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsert(FilingUpsertDTO dto) {
        MisFiling f;
        if (dto.getId() == null) {
            f = new MisFiling();
            f.setStatus("审核中");
            f.setApplyTime(LocalDate.now());
        } else {
            f = filingMapper.selectById(dto.getId());
            if (f == null) {
                return ActionVO.fail("备案记录不存在");
            }
        }
        f.setName(dto.getName());
        f.setIdCardMask(dto.getIdCardMask());
        f.setInsuranceArea(dto.getInsuranceArea());
        f.setHospital(dto.getHospital());
        f.setReason(dto.getReason());
        f.setFromArea(dto.getFromArea());
        f.setValidStart(dto.getValidStart());
        f.setValidEnd(dto.getValidEnd());
        if (dto.getId() == null) {
            filingMapper.insert(f);
        } else {
            filingMapper.updateById(f);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO revoke(Long id) {
        MisFiling f = filingMapper.selectById(id);
        if (f == null) {
            return ActionVO.fail("备案记录不存在");
        }
        if ("已失效".equals(f.getStatus())) {
            return ActionVO.fail("备案已失效");
        }
        f.setStatus("已失效");
        filingMapper.updateById(f);
        return ActionVO.ok();
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
