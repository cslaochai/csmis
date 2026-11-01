package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.ClaimOpDTO;
import com.mis.dto.ClaimQueryDTO;
import com.mis.dto.ClaimUpsertDTO;
import com.mis.entity.MisManualClaim;
import com.mis.mapper.MisManualClaimMapper;
import com.mis.service.ManualClaimService;
import com.mis.vo.ActionVO;
import com.mis.vo.ClaimVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManualClaimServiceImpl implements ManualClaimService {

    private final MisManualClaimMapper claimMapper;

    @Override
    public PageVO<ClaimVO> page(ClaimQueryDTO q) {
        Page<MisManualClaim> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisManualClaim> w = new LambdaQueryWrapper<>();
        if (q.getClaimType() != null) {
            w.eq(MisManualClaim::getClaimType, q.getClaimType());
        }
        if (q.getStatus() != null) {
            w.eq(MisManualClaim::getStatus, q.getStatus());
        }
        w.orderByDesc(MisManualClaim::getId);
        Page<MisManualClaim> res = claimMapper.selectPage(page, w);
        List<ClaimVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
public ActionVO submit(ClaimUpsertDTO dto) {
        MisManualClaim c = new MisManualClaim();
        BeanUtils.copyProperties(dto, c);
        c.setClaimNo("BX" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", (int) (claimMapper.selectCount(null) % 10000L + 1)));
        c.setStatus(0);
        c.setInsurancePay(BigDecimal.ZERO);
        if (c.getPersonalPay() == null) {
            c.setPersonalPay(dto.getTotalAmount() == null ? BigDecimal.ZERO : dto.getTotalAmount());
        }
        claimMapper.insert(c);
        return ActionVO.ok();
    }

    @Override
    public ActionVO audit(ClaimOpDTO dto) {
        MisManualClaim c = claimMapper.selectById(dto.getId());
        if (c == null) {
            return ActionVO.fail("报销单不存在");
        }
        if (c.getStatus() != 0) {
            return ActionVO.fail("该报销单不在待审核状态");
        }
        if (Boolean.TRUE.equals(dto.getPassed())) {
            BigDecimal pay = dto.getInsurancePay() == null ? BigDecimal.ZERO : dto.getInsurancePay();
            c.setInsurancePay(pay);
            c.setStatus(1);
        } else {
            c.setStatus(3);
        }
        c.setAuditOpinion(dto.getAuditOpinion());
        claimMapper.updateById(c);
        return ActionVO.ok();
    }

    @Override
    public ActionVO pay(Long id) {
        MisManualClaim c = claimMapper.selectById(id);
        if (c == null) {
            return ActionVO.fail("报销单不存在");
        }
        if (c.getStatus() != 1) {
            return ActionVO.fail("仅已审核的报销单可拨付");
        }
        c.setStatus(2);
        claimMapper.updateById(c);
        return ActionVO.ok();
    }

    private ClaimVO toVO(MisManualClaim c) {
        ClaimVO vo = new ClaimVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
