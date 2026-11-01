package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.CreditQueryDTO;
import com.mis.dto.CreditScoreDTO;
import com.mis.dto.CreditUpsertDTO;
import com.mis.entity.MifCredit;
import com.mis.mapper.MifCreditMapper;
import com.mis.service.CreditService;
import com.mis.vo.ActionVO;
import com.mis.vo.CreditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final MifCreditMapper creditMapper;

    @Override
    public PageVO<CreditVO> page(CreditQueryDTO q) {
        Page<MifCredit> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifCredit> w = new LambdaQueryWrapper<>();
        if (q.getSubjectType() != null) {
            w.eq(MifCredit::getSubjectType, q.getSubjectType());
        }
        if (q.getPunishType() != null) {
            w.eq(MifCredit::getPunishType, q.getPunishType());
        }
        w.orderByAsc(MifCredit::getId);
        Page<MifCredit> res = creditMapper.selectPage(page, w);
        List<CreditVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsert(CreditUpsertDTO dto) {
        Long dup = creditMapper.selectCount(new LambdaQueryWrapper<MifCredit>()
                .eq(MifCredit::getSubjectType, dto.getSubjectType())
                .eq(MifCredit::getSubjectCode, dto.getSubjectCode())
                .ne(dto.getId() != null, MifCredit::getId, dto.getId()));
        if (dup != null && dup > 0) {
            return ActionVO.fail("该主体编码已存在");
        }
        MifCredit c;
        if (dto.getId() == null) {
            c = new MifCredit();
            c.setViolationCount(dto.getViolationCount() == null ? 0 : dto.getViolationCount());
            c.setPunishType(dto.getPunishType() == null ? 0 : dto.getPunishType());
        } else {
            c = creditMapper.selectById(dto.getId());
            if (c == null) {
                return ActionVO.fail("信用主体不存在");
            }
        }
        c.setSubjectType(dto.getSubjectType());
        c.setSubjectCode(dto.getSubjectCode());
        c.setSubjectName(dto.getSubjectName());
        c.setCreditLevel(dto.getCreditLevel());
        c.setScore(dto.getScore());
        if (dto.getViolationCount() != null) {
            c.setViolationCount(dto.getViolationCount());
        }
        if (dto.getPunishType() != null) {
            c.setPunishType(dto.getPunishType());
        }
        c.setRemark(dto.getRemark());
        if (dto.getId() == null) {
            creditMapper.insert(c);
        } else {
            creditMapper.updateById(c);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO record(CreditScoreDTO dto) {
        MifCredit c = creditMapper.selectById(dto.getId());
        if (c == null) {
            return ActionVO.fail("信用主体不存在");
        }
        c.setScore(dto.getScore());
        c.setCreditLevel(dto.getCreditLevel());
        if (dto.getViolationCount() != null) {
            c.setViolationCount(dto.getViolationCount());
        }
        if (dto.getPunishType() != null) {
            c.setPunishType(dto.getPunishType());
        }
        if (hasText(dto.getRemark())) {
            c.setRemark(dto.getRemark());
        }
        creditMapper.updateById(c);
        return ActionVO.ok();
    }

    private CreditVO toVO(MifCredit c) {
        CreditVO vo = new CreditVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
