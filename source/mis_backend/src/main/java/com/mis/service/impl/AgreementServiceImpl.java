package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.AgreementQueryDTO;
import com.mis.dto.AgreementStatusDTO;
import com.mis.dto.AgreementUpsertDTO;
import com.mis.entity.MisAgreement;
import com.mis.mapper.MisAgreementMapper;
import com.mis.service.AgreementService;
import com.mis.vo.ActionVO;
import com.mis.vo.AgreementVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgreementServiceImpl implements AgreementService {

    private final MisAgreementMapper agreementMapper;

    @Override
    public PageVO<AgreementVO> page(AgreementQueryDTO q) {
        Page<MisAgreement> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisAgreement> w = new LambdaQueryWrapper<>();
        if (q.getMedinsType() != null) {
            w.eq(MisAgreement::getMedinsType, q.getMedinsType());
        }
        if (q.getAgreementStatus() != null) {
            w.eq(MisAgreement::getAgreementStatus, q.getAgreementStatus());
        }
        w.orderByDesc(MisAgreement::getId);
        Page<MisAgreement> res = agreementMapper.selectPage(page, w);
        List<AgreementVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsert(AgreementUpsertDTO dto) {
        Long dup = agreementMapper.selectCount(new LambdaQueryWrapper<MisAgreement>()
                .eq(MisAgreement::getMedinsCode, dto.getMedinsCode())
                .ne(dto.getId() != null, MisAgreement::getId, dto.getId()));
        if (dup != null && dup > 0) {
            return ActionVO.fail("机构编码已存在");
        }
        MisAgreement a;
        if (dto.getId() == null) {
            a = new MisAgreement();
            a.setAgreementStatus(0);
        } else {
            a = agreementMapper.selectById(dto.getId());
            if (a == null) {
                return ActionVO.fail("协议记录不存在");
            }
        }
        a.setMedinsCode(dto.getMedinsCode());
        a.setMedinsName(dto.getMedinsName());
        a.setMedinsType(dto.getMedinsType());
        a.setAgreementNo(dto.getAgreementNo());
        a.setSignDate(dto.getSignDate());
        a.setExpireDate(dto.getExpireDate());
        a.setServiceScope(dto.getServiceScope());
        a.setRemark(dto.getRemark());
        if (dto.getId() == null) {
            agreementMapper.insert(a);
        } else {
            agreementMapper.updateById(a);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO changeStatus(AgreementStatusDTO dto) {
        MisAgreement a = agreementMapper.selectById(dto.getId());
        if (a == null) {
            return ActionVO.fail("协议记录不存在");
        }
        a.setAgreementStatus(dto.getAgreementStatus());
        if (hasText(dto.getRemark())) {
            a.setRemark(dto.getRemark());
        }
        agreementMapper.updateById(a);
        return ActionVO.ok();
    }

    @Override
    public ActionVO assess(Long id, Integer score) {
        MisAgreement a = agreementMapper.selectById(id);
        if (a == null) {
            return ActionVO.fail("协议记录不存在");
        }
        a.setAnnualScore(score);
        agreementMapper.updateById(a);
        return ActionVO.ok();
    }

    private AgreementVO toVO(MisAgreement a) {
        AgreementVO vo = new AgreementVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
