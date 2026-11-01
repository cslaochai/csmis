package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageResult;
import com.mis.common.exception.BusinessException;
import com.mis.dto.PersonQueryDTO;
import com.mis.dto.PersonSaveDTO;
import com.mis.dto.PersonStatusDTO;
import com.mis.entity.MisInsuredPerson;
import com.mis.mapper.MisInsuredPersonMapper;
import com.mis.service.InsuredPersonService;
import com.mis.vo.InsuredPersonVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InsuredPersonServiceImpl implements InsuredPersonService {

    private final MisInsuredPersonMapper personMapper;

    public InsuredPersonServiceImpl(MisInsuredPersonMapper personMapper) {
        this.personMapper = personMapper;
    }

    @Override
    public InsuredPersonVO query(PersonQueryDTO queryDTO) {
        LambdaQueryWrapper<MisInsuredPerson> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getIdCard())) {
                wrapper.eq(MisInsuredPerson::getIdCard, queryDTO.getIdCard());
            } else if (StringUtils.hasText(queryDTO.getInsuranceNo())) {
                wrapper.eq(MisInsuredPerson::getInsuranceNo, queryDTO.getInsuranceNo());
            } else if (StringUtils.hasText(queryDTO.getName())) {
                wrapper.eq(MisInsuredPerson::getName, queryDTO.getName());
            }
        }
        wrapper.last("limit 1");
        MisInsuredPerson person = personMapper.selectOne(wrapper);
        return person == null ? null : toVO(person);
    }

    @Override
    public PageResult<InsuredPersonVO> page(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<MisInsuredPerson> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MisInsuredPerson::getName, keyword)
                    .or().like(MisInsuredPerson::getIdCard, keyword)
                    .or().like(MisInsuredPerson::getInsuranceNo, keyword));
        }
        wrapper.orderByDesc(MisInsuredPerson::getId);
        Page<MisInsuredPerson> page = personMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<InsuredPersonVO> vos = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getPages(), vos);
    }

    @Override
    public InsuredPersonVO save(PersonSaveDTO saveDTO) {
        MisInsuredPerson exist = personMapper.selectOne(new LambdaQueryWrapper<MisInsuredPerson>()
                .eq(MisInsuredPerson::getIdCard, saveDTO.getIdCard())
                .last("limit 1"));
        if (exist != null) {
            BeanUtils.copyProperties(saveDTO, exist);
            personMapper.updateById(exist);
            return toVO(exist);
        }
        MisInsuredPerson person = new MisInsuredPerson();
        BeanUtils.copyProperties(saveDTO, person);
        if (person.getPersonStatus() == null) {
            person.setPersonStatus(1);
        }
        if (person.getAccountBalance() == null) {
            person.setAccountBalance(BigDecimal.ZERO);
        }
        personMapper.insert(person);
        return toVO(person);
    }

    @Override
    public InsuredPersonVO changeStatus(PersonStatusDTO statusDTO) {
        if (statusDTO.getPersonStatus() == null || (statusDTO.getPersonStatus() != 0 && statusDTO.getPersonStatus() != 1)) {
            throw new BusinessException("参保状态仅支持 1-正常参保 / 0-停保");
        }
        MisInsuredPerson person = personMapper.selectById(statusDTO.getId());
        if (person == null) {
            throw new BusinessException("参保人不存在");
        }
        person.setPersonStatus(statusDTO.getPersonStatus());
        person.setStatusRemark(statusDTO.getRemark());
        personMapper.updateById(person);
        return toVO(person);
    }

    private InsuredPersonVO toVO(MisInsuredPerson person) {
        InsuredPersonVO vo = new InsuredPersonVO();
        BeanUtils.copyProperties(person, vo);
        return vo;
    }
}
