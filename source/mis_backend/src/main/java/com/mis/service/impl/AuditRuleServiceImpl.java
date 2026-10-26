package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.RuleQueryDTO;
import com.mis.dto.RuleStatusDTO;
import com.mis.dto.RuleUpsertDTO;
import com.mis.entity.MssAuditRule;
import com.mis.mapper.MssAuditRuleMapper;
import com.mis.service.AuditRuleService;
import com.mis.vo.ActionVO;
import com.mis.vo.AuditRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditRuleServiceImpl implements AuditRuleService {

    private final MssAuditRuleMapper ruleMapper;

    @Override
    public PageVO<AuditRuleVO> rulePage(RuleQueryDTO q) {
        Page<MssAuditRule> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MssAuditRule> w = new LambdaQueryWrapper<>();
        if (hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(MssAuditRule::getRuleName, kw)
                    .or().like(MssAuditRule::getRuleCode, kw));
        }
        if (hasText(q.getRuleType())) {
            w.eq(MssAuditRule::getRuleType, q.getRuleType());
        }
        if (q.getStatus() != null) {
            w.eq(MssAuditRule::getStatus, q.getStatus());
        }
        w.orderByAsc(MssAuditRule::getId);
        Page<MssAuditRule> res = ruleMapper.selectPage(page, w);
        List<AuditRuleVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsert(RuleUpsertDTO dto) {
        // 规则编码唯一（排除自身）
        Long dup = ruleMapper.selectCount(new LambdaQueryWrapper<MssAuditRule>()
                .eq(MssAuditRule::getRuleCode, dto.getRuleCode())
                .ne(dto.getId() != null, MssAuditRule::getId, dto.getId()));
        if (dup != null && dup > 0) {
            return ActionVO.fail("规则编码已存在");
        }
        MssAuditRule rule;
        if (dto.getId() == null) {
            rule = new MssAuditRule();
            rule.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        } else {
            rule = ruleMapper.selectById(dto.getId());
            if (rule == null) {
                return ActionVO.fail("规则不存在");
            }
        }
        rule.setRuleCode(dto.getRuleCode());
        rule.setRuleName(dto.getRuleName());
        rule.setRuleType(dto.getRuleType());
        rule.setTarget(dto.getTarget());
        rule.setSeverity(dto.getSeverity());
        rule.setParams(dto.getParams());
        rule.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            rule.setStatus(dto.getStatus());
        }
        if (dto.getId() == null) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO changeStatus(RuleStatusDTO dto) {
        MssAuditRule rule = ruleMapper.selectById(dto.getId());
        if (rule == null) {
            return ActionVO.fail("规则不存在");
        }
        rule.setStatus(dto.getStatus());
        ruleMapper.updateById(rule);
        return ActionVO.ok();
    }

    @Override
    public List<AuditRuleVO> enabledRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<MssAuditRule>()
                        .eq(MssAuditRule::getStatus, 1)
                        .orderByAsc(MssAuditRule::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    private AuditRuleVO toVO(MssAuditRule rule) {
        AuditRuleVO vo = new AuditRuleVO();
        BeanUtils.copyProperties(rule, vo);
        return vo;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
