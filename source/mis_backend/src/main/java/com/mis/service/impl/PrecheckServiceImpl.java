package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.PrecheckResultHandleDTO;
import com.mis.dto.PrecheckResultQueryDTO;
import com.mis.dto.PrecheckRuleQueryDTO;
import com.mis.dto.PrecheckRuleStatusDTO;
import com.mis.dto.PrecheckRuleUpsertDTO;
import com.mis.engine.MonitorEngine;
import com.mis.engine.SettleEngine;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MisPendingSettle;
import com.mis.entity.MifPrecheckResult;
import com.mis.entity.MifPrecheckRule;
import com.mis.mapper.MisFeeItemMapper;
import com.mis.mapper.MisPendingSettleMapper;
import com.mis.mapper.MifPrecheckResultMapper;
import com.mis.mapper.MifPrecheckRuleMapper;
import com.mis.service.PrecheckService;
import com.mis.vo.ActionVO;
import com.mis.vo.DecVO;
import com.mis.vo.PrecheckResultVO;
import com.mis.vo.PrecheckRuleVO;
import com.mis.vo.PrecheckVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事前提醒 / 事中拦截（三道防线前两道）
 * <p>真实判定由 MonitorEngine 完成：本类负责规则维护、命中留痕与处置状态流转。
 */
@Service
@RequiredArgsConstructor
public class PrecheckServiceImpl implements PrecheckService {

    private final MifPrecheckRuleMapper ruleMapper;
    private final MifPrecheckResultMapper resultMapper;
    private final MisPendingSettleMapper pendingMapper;
    private final MisFeeItemMapper feeItemMapper;
    private final MonitorEngine monitorEngine;
    private final SettleEngine settleEngine;

    // ------------------------------------------------------------------ 规则维护

    @Override
    public PageVO<PrecheckRuleVO> pageRule(PrecheckRuleQueryDTO q) {
        Page<MifPrecheckRule> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifPrecheckRule> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifPrecheckRule::getRuleCode, k).or().like(MifPrecheckRule::getRuleName, k));
        }
        if (StringUtils.hasText(q.getTriggerStage())) {
            w.eq(MifPrecheckRule::getTriggerStage, q.getTriggerStage());
        }
        if (q.getStatus() != null) {
            w.eq(MifPrecheckRule::getStatus, q.getStatus());
        }
        w.orderByDesc(MifPrecheckRule::getId);
        Page<MifPrecheckRule> res = ruleMapper.selectPage(page, w);
        List<PrecheckRuleVO> vos = res.getRecords().stream().map(this::toRuleVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsertRule(PrecheckRuleUpsertDTO dto) {
        Long dup = ruleMapper.selectCount(new LambdaQueryWrapper<MifPrecheckRule>()
                .eq(MifPrecheckRule::getRuleCode, dto.getRuleCode())
                .ne(dto.getId() != null, MifPrecheckRule::getId, dto.getId()));
        if (dup != null && dup > 0) {
            return ActionVO.fail("规则编码已存在");
        }
        MifPrecheckRule r;
        if (dto.getId() == null) {
            r = new MifPrecheckRule();
        } else {
            r = ruleMapper.selectById(dto.getId());
            if (r == null) {
                return ActionVO.fail("规则不存在");
            }
        }
        r.setRuleCode(dto.getRuleCode());
        r.setRuleName(dto.getRuleName());
        r.setTriggerStage(dto.getTriggerStage());
        r.setRiskLevel(dto.getRiskLevel());
        r.setPromptText(dto.getPromptText());
        r.setBlockFlag(dto.getBlockFlag());
        r.setCategory(dto.getCategory());
        r.setParams(dto.getParams());
        r.setStatus(dto.getStatus());
        r.setDescription(dto.getDescription());
        r.setUpdateTime(LocalDateTime.now());
        if (dto.getId() == null) {
            r.setCreateTime(LocalDateTime.now());
            ruleMapper.insert(r);
        } else {
            ruleMapper.updateById(r);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO changeRuleStatus(PrecheckRuleStatusDTO dto) {
        MifPrecheckRule r = ruleMapper.selectById(dto.getId());
        if (r == null) {
            return ActionVO.fail("规则不存在");
        }
        r.setStatus(dto.getStatus());
        r.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(r);
        return ActionVO.ok();
    }

    // ------------------------------------------------------------------ 命中结果

    @Override
    public PageVO<PrecheckResultVO> pageResult(PrecheckResultQueryDTO q) {
        Page<MifPrecheckResult> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifPrecheckResult> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifPrecheckResult::getBizNo, k).or().like(MifPrecheckResult::getMedinsName, k)
                    .or().like(MifPrecheckResult::getInsuredName, k).or().like(MifPrecheckResult::getInsuredIdCard, k));
        }
        if (StringUtils.hasText(q.getTriggerStage())) {
            w.eq(MifPrecheckResult::getTriggerStage, q.getTriggerStage());
        }
        if (q.getHandleStatus() != null) {
            w.eq(MifPrecheckResult::getHandleStatus, q.getHandleStatus());
        }
        w.orderByDesc(MifPrecheckResult::getId);
        Page<MifPrecheckResult> res = resultMapper.selectPage(page, w);
        List<PrecheckResultVO> vos = res.getRecords().stream().map(this::toResultVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    /**
     * 真实批量评估：对待结算业务逐笔跑启用的事前/事中规则，命中即留痕（按 规则+业务单号 幂等去重）。
     */
    @Override
    public ActionVO evaluate() {
        List<MifPrecheckRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<MifPrecheckRule>()
                .eq(MifPrecheckRule::getStatus, 1));
        if (rules.isEmpty()) {
            return ActionVO.fail("无启用的事前/事中规则");
        }
        List<MisPendingSettle> pendings = pendingMapper.selectList(new LambdaQueryWrapper<MisPendingSettle>()
                .eq(MisPendingSettle::getStatus, 1)
                .orderByAsc(MisPendingSettle::getId));
        if (pendings.isEmpty()) {
            return ActionVO.fail("暂无待结算业务，无法评估");
        }
        int count = 0;
        for (MisPendingSettle p : pendings) {
            List<MisFeeItem> items = itemsOf(p.getId());
            DecVO dec = settleEngine.compute(items, p.getInsuranceType());
            List<MonitorEngine.Hit> hits = monitorEngine.evaluate(rules, p, items, dec);
            for (MonitorEngine.Hit h : hits) {
                if (saveHit(h, p)) {
                    count++;
                }
            }
        }
        return ActionVO.matched(count);
    }

    @Override
    public ActionVO handleResult(PrecheckResultHandleDTO dto) {
        MifPrecheckResult r = resultMapper.selectById(dto.getId());
        if (r == null) {
            return ActionVO.fail("命中结果不存在");
        }
        if (r.getHandleStatus() != null && r.getHandleStatus() == 1) {
            return ActionVO.fail("该命中结果已处理，不可重复操作");
        }
        r.setHandleStatus(1);
        r.setHandleOpinion(buildOpinion(dto.getHandleOpinion()));
        r.setUpdateTime(LocalDateTime.now());
        resultMapper.updateById(r);
        return ActionVO.ok();
    }

    // ------------------------------------------------------------------ 事前 / 事中

    /** 事前提醒：不落库，只返回提醒明细（阻断项仅作强提醒，不阻断经办确认） */
    @Override
    public PrecheckVO check(MisPendingSettle pending, List<MisFeeItem> items, DecVO dec) {
        List<MifPrecheckRule> rules = rulesOfStage("before_settle");
        List<MonitorEngine.Hit> hits = monitorEngine.evaluate(rules, pending, items, dec);
        return buildVO(hits);
    }

    /** 事前提醒（按业务单号）：自动装载待结算业务与费用明细后评估 */
    @Override
    public PrecheckVO checkByBizNo(String bizNo) {
        MisPendingSettle p = pendingMapper.selectOne(new LambdaQueryWrapper<MisPendingSettle>()
                .eq(MisPendingSettle::getBizNo, bizNo)
                .eq(MisPendingSettle::getStatus, 1)
                .last("limit 1"));
        if (p == null) {
            PrecheckVO vo = new PrecheckVO();
            vo.setPass(true);
            vo.setBlockFlag(0);
            vo.setHitCount(0);
            vo.setMaxRiskLevel(0);
            vo.setSuggest("放行");
            vo.setHits(new ArrayList<>());
            return vo;
        }
        List<MisFeeItem> items = itemsOf(p.getId());
        DecVO dec = settleEngine.compute(items, p.getInsuranceType());
        return check(p, items, dec);
    }

    /** 事中拦截：落库留痕，阻断项对应拒付，高风险项建议转人工复核 */
    @Override
    public PrecheckVO intercept(MisPendingSettle pending, List<MisFeeItem> items, DecVO dec) {
        List<MifPrecheckRule> rules = rulesOfStage("in_settle");
        List<MonitorEngine.Hit> hits = monitorEngine.evaluate(rules, pending, items, dec);
        for (MonitorEngine.Hit h : hits) {
            saveHit(h, pending);
        }
        return buildVO(hits);
    }

    // ------------------------------------------------------------------ 内部

    private List<MifPrecheckRule> rulesOfStage(String stage) {
        return ruleMapper.selectList(new LambdaQueryWrapper<MifPrecheckRule>()
                .eq(MifPrecheckRule::getStatus, 1)
                .eq(MifPrecheckRule::getTriggerStage, stage));
    }

    private List<MisFeeItem> itemsOf(Long pendingId) {
        return feeItemMapper.selectList(new LambdaQueryWrapper<MisFeeItem>()
                .eq(MisFeeItem::getOwnerType, 1)
                .eq(MisFeeItem::getOwnerId, pendingId)
                .orderByAsc(MisFeeItem::getId));
    }

    /**
     * 落库命中留痕，按（规则编码 + 业务单号 + 提示语）幂等去重，已存在则跳过。
     */
    private boolean saveHit(MonitorEngine.Hit h, MisPendingSettle p) {
        if (h.getRuleCode() == null) {
            return false;
        }
        Long dup = resultMapper.selectCount(new LambdaQueryWrapper<MifPrecheckResult>()
                .eq(MifPrecheckResult::getRuleCode, h.getRuleCode())
                .eq(MifPrecheckResult::getBizNo, h.getBizNo() == null ? "" : h.getBizNo())
                .eq(MifPrecheckResult::getPromptText, h.getPromptText() == null ? "" : h.getPromptText()));
        if (dup != null && dup > 0) {
            return false;
        }
        MifPrecheckResult r = new MifPrecheckResult();
        r.setRuleId(h.getRuleId());
        r.setRuleCode(h.getRuleCode());
        r.setRuleName(h.getRuleName());
        r.setTriggerStage(h.getTriggerStage());
        r.setBizNo(h.getBizNo());
        r.setMedinsCode(h.getMedinsCode());
        r.setMedinsName(h.getMedinsName());
        r.setInsuredIdCard(h.getInsuredIdCard());
        r.setInsuredName(h.getInsuredName());
        r.setPromptText(h.getPromptText());
        r.setBlockFlag(h.getBlockFlag());
        r.setRiskLevel(h.getRiskLevel());
        r.setHandleStatus(0);
        r.setHitTime(LocalDateTime.now());
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        resultMapper.insert(r);
        return true;
    }

    private PrecheckVO buildVO(List<MonitorEngine.Hit> hits) {
        List<PrecheckResultVO> vos = new ArrayList<>();
        boolean blocked = false;
        int maxRisk = 0;
        if (hits != null) {
            for (MonitorEngine.Hit h : hits) {
                PrecheckResultVO vo = new PrecheckResultVO();
                vo.setRuleId(h.getRuleId());
                vo.setRuleCode(h.getRuleCode());
                vo.setRuleName(h.getRuleName());
                vo.setTriggerStage(h.getTriggerStage());
                vo.setBizNo(h.getBizNo());
                vo.setMedinsCode(h.getMedinsCode());
                vo.setMedinsName(h.getMedinsName());
                vo.setInsuredIdCard(h.getInsuredIdCard());
                vo.setInsuredName(h.getInsuredName());
                vo.setPromptText(h.getPromptText());
                vo.setBlockFlag(h.getBlockFlag());
                vo.setRiskLevel(h.getRiskLevel());
                vo.setHandleStatus(0);
                vos.add(vo);
                if (h.getBlockFlag() != null && h.getBlockFlag() == 1) {
                    blocked = true;
                }
                if (h.getRiskLevel() != null && h.getRiskLevel() > maxRisk) {
                    maxRisk = h.getRiskLevel();
                }
            }
        }
        PrecheckVO vo = new PrecheckVO();
        vo.setPass(!blocked);
        vo.setBlockFlag(blocked ? 1 : 0);
        vo.setHitCount(vos.size());
        vo.setMaxRiskLevel(maxRisk);
        vo.setHits(vos);
        vo.setSuggest(suggest(blocked, maxRisk, vos.size()));
        return vo;
    }

    private String suggest(boolean blocked, int maxRisk, int hitCount) {
        if (hitCount == 0) {
            return "放行";
        }
        if (blocked) {
            return "阻断结算";
        }
        if (maxRisk >= 3) {
            return "转人工复核";
        }
        return "提醒后放行";
    }

    private String buildOpinion(String opinion) {
        String user = UserContext.username();
        String stamp = LocalDateTime.now().toString().replace('T', ' ').substring(0, 16);
        String who = user == null || user.isBlank() ? "系统" : user;
        return "[" + who + " " + stamp + "] " + (opinion == null || opinion.isBlank() ? "已确认" : opinion);
    }

    private PrecheckRuleVO toRuleVO(MifPrecheckRule r) {
        PrecheckRuleVO vo = new PrecheckRuleVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }

    private PrecheckResultVO toResultVO(MifPrecheckResult r) {
        PrecheckResultVO vo = new PrecheckResultVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}
