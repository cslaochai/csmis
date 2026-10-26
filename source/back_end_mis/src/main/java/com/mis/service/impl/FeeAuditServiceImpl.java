package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.AuditQueryDTO;
import com.mis.entity.MssFeeAudit;
import com.mis.mapper.MssFeeAuditMapper;
import com.mis.service.FeeAuditService;
import com.mis.vo.ActionVO;
import com.mis.vo.FeeAuditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeAuditServiceImpl implements FeeAuditService {

    private final MssFeeAuditMapper auditMapper;

    @Override
    public PageVO<FeeAuditVO> auditPage(AuditQueryDTO q) {
        Page<MssFeeAudit> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MssFeeAudit> w = new LambdaQueryWrapper<>();
        if (hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(MssFeeAudit::getBillNo, kw)
                    .or().like(MssFeeAudit::getPatientName, kw)
                    .or().like(MssFeeAudit::getItemName, kw));
        }
        if (hasText(q.getPayClass())) {
            w.eq(MssFeeAudit::getPayClass, q.getPayClass());
        }
        if (hasText(q.getStatus())) {
            w.eq(MssFeeAudit::getStatus, q.getStatus());
        }
        w.orderByDesc(MssFeeAudit::getId);
        Page<MssFeeAudit> res = auditMapper.selectPage(page, w);
        List<FeeAuditVO> vos = res.getRecords().stream().map(a -> {
            FeeAuditVO vo = new FeeAuditVO();
            BeanUtils.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO pass(Long id) {
        return changeStatus(id, "已通过");
    }

    @Override
    public ActionVO reject(Long id) {
        return changeStatus(id, "已驳回");
    }

    private ActionVO changeStatus(Long id, String status) {
        MssFeeAudit a = auditMapper.selectById(id);
        if (a == null) {
            return ActionVO.fail("审核任务不存在");
        }
        if (!"待审核".equals(a.getStatus())) {
            return ActionVO.fail("该任务已审核，不可重复操作");
        }
        a.setStatus(status);
        auditMapper.updateById(a);
        return ActionVO.ok();
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
