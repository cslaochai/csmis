package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.AuditFlowDTO;
import com.mis.dto.AuditQueryDTO;
import com.mis.entity.MisFeeAudit;
import com.mis.mapper.MisFeeAuditMapper;
import com.mis.service.FeeAuditService;
import com.mis.vo.ActionVO;
import com.mis.vo.FeeAuditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import com.mis.service.impl.TodoServiceImpl;

@Service
@RequiredArgsConstructor
public class FeeAuditServiceImpl implements FeeAuditService {

    private final MisFeeAuditMapper auditMapper;

    @Override
    public PageVO<FeeAuditVO> auditPage(AuditQueryDTO q) {
        Page<MisFeeAudit> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisFeeAudit> w = new LambdaQueryWrapper<>();
        if (hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(MisFeeAudit::getBillNo, kw)
                    .or().like(MisFeeAudit::getPatientName, kw)
                    .or().like(MisFeeAudit::getItemName, kw));
        }
        if (hasText(q.getPayClass())) {
            w.eq(MisFeeAudit::getPayClass, q.getPayClass());
        }
        if (hasText(q.getStatus())) {
            w.eq(MisFeeAudit::getStatus, q.getStatus());
        }
        w.orderByDesc(MisFeeAudit::getId);
        Page<MisFeeAudit> res = auditMapper.selectPage(page, w);
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
        MisFeeAudit a = auditMapper.selectById(id);
        if (a == null) {
            return ActionVO.fail("审核任务不存在");
        }
        if (!"待审核".equals(a.getStatus())) {
            return ActionVO.fail("该任务已审核，不可重复操作");
        }
        a.setStatus(status);
        auditMapper.updateById(a);
        // 审核办结 → 关闭对应待办
        TodoServiceImpl.closeTodo("mis_fee_audit", a.getId(), "费用审核已" + status);
        return ActionVO.ok();
    }

    @Override
    public ActionVO appeal(AuditFlowDTO dto) {
        MisFeeAudit a = auditMapper.selectById(dto.getId());
        if (a == null) {
            return ActionVO.fail("审核任务不存在");
        }
        if (!"已驳回".equals(a.getStatus())) {
            return ActionVO.fail("仅被驳回的审核任务可提起申诉");
        }
        a.setAppealReason(dto.getAppealReason());
        a.setStatus("已申诉");
        auditMapper.updateById(a);
        return ActionVO.ok();
    }

    @Override
    public ActionVO review(AuditFlowDTO dto) {
        MisFeeAudit a = auditMapper.selectById(dto.getId());
        if (a == null) {
            return ActionVO.fail("审核任务不存在");
        }
        if (!"已申诉".equals(a.getStatus())) {
            return ActionVO.fail("仅申诉中的审核任务可复审");
        }
        if (!"复审通过".equals(dto.getReviewResult()) && !"复审维持".equals(dto.getReviewResult())) {
            return ActionVO.fail("复审结论必须是 复审通过 / 复审维持");
        }
        a.setReviewResult(dto.getReviewResult());
        a.setReviewOpinion(dto.getReviewOpinion());
        a.setStatus(dto.getReviewResult());
        auditMapper.updateById(a);
        return ActionVO.ok();
    }

    @Override
    public ActionVO confer(AuditFlowDTO dto) {
        MisFeeAudit a = auditMapper.selectById(dto.getId());
        if (a == null) {
            return ActionVO.fail("审核任务不存在");
        }
        if (!"已申诉".equals(a.getStatus()) && !"复审通过".equals(a.getStatus()) && !"复审维持".equals(a.getStatus())) {
            return ActionVO.fail("仅申诉中或已复审的任务可合议");
        }
        a.setConferOpinion(dto.getConferOpinion());
        a.setStatus("已合议");
        auditMapper.updateById(a);
        return ActionVO.ok();
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
