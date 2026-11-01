package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.ChronicAuditDTO;
import com.mis.dto.ChronicQueryDTO;
import com.mis.dto.ChronicUpsertDTO;
import com.mis.entity.MisInsuredPerson;
import com.mis.entity.MisChronic;
import com.mis.mapper.MisInsuredPersonMapper;
import com.mis.mapper.MisChronicMapper;
import com.mis.service.ChronicService;
import com.mis.vo.ActionVO;
import com.mis.vo.ChronicVO;
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
 * 门诊慢特病（门诊特殊病种）待遇认定：申请 → 审核认定 → 待遇有效期
 */
@Service
@RequiredArgsConstructor
public class ChronicServiceImpl implements ChronicService {

    private final MisChronicMapper mapper;
    private final MisInsuredPersonMapper personMapper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Override
    public PageVO<ChronicVO> page(ChronicQueryDTO q) {
        Page<MisChronic> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisChronic> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MisChronic::getRecordNo, k).or().like(MisChronic::getPersonName, k)
                    .or().like(MisChronic::getIdCard, k).or().like(MisChronic::getDiseaseName, k));
        }
        if (q.getDiseaseLevel() != null) {
            w.eq(MisChronic::getDiseaseLevel, q.getDiseaseLevel());
        }
        if (q.getStatus() != null) {
            w.eq(MisChronic::getStatus, q.getStatus());
        }
        w.orderByDesc(MisChronic::getId);
        Page<MisChronic> res = mapper.selectPage(page, w);
        List<ChronicVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsert(ChronicUpsertDTO dto) {
        MisInsuredPerson person = personMapper.selectOne(new LambdaQueryWrapper<MisInsuredPerson>()
                .eq(MisInsuredPerson::getIdCard, dto.getIdCard())
                .last("limit 1"));
        if (person == null) {
            return ActionVO.fail("参保人未在参保库登记，无法申请待遇认定");
        }
        // 同一参保人同病种不允许重复申请（已认定的也不重复）
        Long dup = mapper.selectCount(new LambdaQueryWrapper<MisChronic>()
                .eq(MisChronic::getIdCard, dto.getIdCard())
                .eq(MisChronic::getDiseaseName, dto.getDiseaseName())
                .ne(MisChronic::getStatus, 3));
        if (dup != null && dup > 0) {
            return ActionVO.fail("该参保人已存在相同病种的认定申请或已认定记录");
        }
        MisChronic c;
        if (dto.getId() == null) {
            c = new MisChronic();
            c.setRecordNo("MB" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%05d", SEQ.incrementAndGet()));
            c.setStatus(1);
        } else {
            c = mapper.selectById(dto.getId());
            if (c == null) {
                return ActionVO.fail("认定单不存在");
            }
            if (c.getStatus() != null && c.getStatus() != 1) {
                return ActionVO.fail("该申请已审核，不能修改");
            }
        }
        c.setPersonName(dto.getPersonName());
        c.setIdCard(dto.getIdCard());
        c.setInsuranceNo(dto.getInsuranceNo() != null ? dto.getInsuranceNo() : person.getInsuranceNo());
        c.setDiseaseCode(dto.getDiseaseCode());
        c.setDiseaseName(dto.getDiseaseName());
        c.setDiseaseLevel(dto.getDiseaseLevel());
        c.setDoctor(dto.getDoctor());
        c.setHospital(dto.getHospital());
        c.setApplyDate(dto.getApplyDate() != null ? dto.getApplyDate() : LocalDate.now());
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
    public ActionVO audit(ChronicAuditDTO dto) {
        MisChronic c = mapper.selectById(dto.getId());
        if (c == null) {
            return ActionVO.fail("认定单不存在");
        }
        if (c.getStatus() != null && c.getStatus() != 1) {
            return ActionVO.fail("该申请已审核，不能重复操作");
        }
        int result = dto.getResult();
        if (result != 2 && result != 3 && result != 4) {
            return ActionVO.fail("审核结论必须是 认定通过 / 不通过 / 复审通过");
        }
        LocalDateTime now = LocalDateTime.now();
        c.setStatus(result);
        c.setReviewer(UserContext.username());
        c.setReviewOpinion(dto.getOpinion());
        c.setReviewTime(now);
        if (result == 2 || result == 4) {
            // 认定通过：核定待遇有效期（默认 1 年）
            c.setValidFrom(dto.getValidFrom() != null ? dto.getValidFrom() : LocalDate.now());
            c.setValidTo(dto.getValidTo() != null ? dto.getValidTo() : LocalDate.now().plusYears(1));
        }
        c.setUpdateTime(now);
        mapper.updateById(c);
        return ActionVO.ok();
    }

    private ChronicVO toVO(MisChronic c) {
        ChronicVO vo = new ChronicVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
