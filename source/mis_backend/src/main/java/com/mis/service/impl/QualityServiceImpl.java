package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.QualityResultHandleDTO;
import com.mis.dto.QualityResultQueryDTO;
import com.mis.dto.QualityRuleQueryDTO;
import com.mis.dto.QualityRuleUpsertDTO;
import com.mis.engine.QualityEngine;
import com.mis.entity.MisBill;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MifQualityResult;
import com.mis.entity.MifQualityRule;
import com.mis.entity.MifRecover;
import com.mis.mapper.MisBillMapper;
import com.mis.mapper.MisFeeItemMapper;
import com.mis.mapper.MifQualityResultMapper;
import com.mis.mapper.MifQualityRuleMapper;
import com.mis.mapper.MifRecoverMapper;
import com.mis.service.QualityService;
import com.mis.vo.ActionVO;
import com.mis.vo.QualityResultVO;
import com.mis.vo.QualityRuleVO;
import com.mis.vo.QualityStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.mis.service.SettleListService;

/**
 * 结算清单质控：规则维护 + 清单真实质控 + 命中留痕与处理（确认可联动扣款）
 */
@Service
@RequiredArgsConstructor
public class QualityServiceImpl implements QualityService {

    private final MifQualityRuleMapper ruleMapper;
    private final MifQualityResultMapper resultMapper;
    private final MisBillMapper billMapper;
    private final MisFeeItemMapper feeItemMapper;
    private final MifRecoverMapper recoverMapper;
    private final QualityEngine qualityEngine;
    private final SettleListService settleListService;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    // ------------------------------------------------------------------ 规则

    @Override
    public PageVO<QualityRuleVO> pageRule(QualityRuleQueryDTO q) {
        Page<MifQualityRule> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifQualityRule> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifQualityRule::getRuleCode, k).or().like(MifQualityRule::getRuleName, k));
        }
        if (StringUtils.hasText(q.getRuleType())) {
            w.eq(MifQualityRule::getRuleType, q.getRuleType());
        }
        if (q.getSeverity() != null) {
            w.eq(MifQualityRule::getSeverity, q.getSeverity());
        }
        if (q.getStatus() != null) {
            w.eq(MifQualityRule::getStatus, q.getStatus());
        }
        w.orderByDesc(MifQualityRule::getId);
        Page<MifQualityRule> res = ruleMapper.selectPage(page, w);
        List<QualityRuleVO> vos = res.getRecords().stream().map(this::toRuleVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsertRule(QualityRuleUpsertDTO dto) {
        Long dup = ruleMapper.selectCount(new LambdaQueryWrapper<MifQualityRule>()
                .eq(MifQualityRule::getRuleCode, dto.getRuleCode())
                .ne(dto.getId() != null, MifQualityRule::getId, dto.getId()));
        if (dup != null && dup > 0) {
            return ActionVO.fail("规则编码已存在");
        }
        MifQualityRule r;
        if (dto.getId() == null) {
            r = new MifQualityRule();
        } else {
            r = ruleMapper.selectById(dto.getId());
            if (r == null) {
                return ActionVO.fail("质控规则不存在");
            }
        }
        r.setRuleCode(dto.getRuleCode());
        r.setRuleName(dto.getRuleName());
        r.setRuleType(dto.getRuleType());
        r.setSeverity(dto.getSeverity());
        r.setDeductRatio(dto.getDeductRatio());
        r.setDeductType(dto.getDeductType());
        r.setDeductAmount(dto.getDeductAmount());
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
    public ActionVO changeRuleStatus(Long id, Integer status) {
        MifQualityRule r = ruleMapper.selectById(id);
        if (r == null) {
            return ActionVO.fail("质控规则不存在");
        }
        r.setStatus(status);
        r.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(r);
        return ActionVO.ok();
    }

    // ------------------------------------------------------------------ 结果

    @Override
    public PageVO<QualityResultVO> pageResult(QualityResultQueryDTO q) {
        Page<MifQualityResult> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifQualityResult> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifQualityResult::getBillNo, k).or().like(MifQualityResult::getItemName, k)
                    .or().like(MifQualityResult::getRuleName, k).or().like(MifQualityResult::getMedinsName, k));
        }
        if (StringUtils.hasText(q.getRuleCode())) {
            w.eq(MifQualityResult::getRuleCode, q.getRuleCode());
        }
        if (StringUtils.hasText(q.getBillNo())) {
            w.eq(MifQualityResult::getBillNo, q.getBillNo());
        }
        if (q.getSeverity() != null) {
            w.eq(MifQualityResult::getSeverity, q.getSeverity());
        }
        if (q.getHandleStatus() != null) {
            w.eq(MifQualityResult::getHandleStatus, q.getHandleStatus());
        }
        w.orderByDesc(MifQualityResult::getId);
        Page<MifQualityResult> res = resultMapper.selectPage(page, w);
        List<QualityResultVO> vos = res.getRecords().stream().map(this::toResultVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    /**
     * 对已结算结算单执行清单质控，命中留痕（同一结算单 + 规则 + 明细 幂等去重）。
     */
    @Override
    public ActionVO check() {
        List<MifQualityRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<MifQualityRule>()
                .eq(MifQualityRule::getStatus, 1));
        if (rules.isEmpty()) {
            return ActionVO.fail("无启用质控规则");
        }
        List<MisBill> bills = billMapper.selectList(new LambdaQueryWrapper<MisBill>()
                .ne(MisBill::getStatus, "已退费"));
        if (bills.isEmpty()) {
            return ActionVO.fail("暂无已结算结算单，无法质控");
        }
        QualityEngine.Ctx ctx = qualityEngine.loadContext();
        Map<Long, List<MisFeeItem>> itemsByBill = loadItemsByBill();
        int count = 0;
        for (MisBill b : bills) {
            List<MisFeeItem> items = itemsByBill.get(b.getId());
            if (items == null || items.isEmpty()) {
                continue;
            }
            for (QualityEngine.Hit h : qualityEngine.check(b, items, rules, ctx)) {
                if (saveHit(h)) {
                    count++;
                }
            }
            // 清单质控状态回写：有命中 → 质控异常(2)，无命中 → 通过(1)
            settleListService.updateCheckStatus(b.getId(), hasHit(b.getId()) ? 2 : 1);
        }
        return ActionVO.matched(count);
    }

    /** 该结算单是否存在未处理的质控命中 */
    private boolean hasHit(Long billId) {
        Long n = resultMapper.selectCount(new LambdaQueryWrapper<MifQualityResult>()
                .eq(MifQualityResult::getBillId, billId));
        return n != null && n > 0;
    }

    @Override
    public ActionVO handleResult(QualityResultHandleDTO dto) {
        MifQualityResult r = resultMapper.selectById(dto.getId());
        if (r == null) {
            return ActionVO.fail("质控结果不存在");
        }
        if (r.getHandleStatus() != null && r.getHandleStatus() != 0) {
            return ActionVO.fail("该质控结果已处理，不能重复操作");
        }
        r.setHandleStatus(dto.getHandleStatus());
        r.setHandleOpinion(buildOpinion(dto.getHandleOpinion()));
        r.setHandleUser(UserContext.username());
        r.setHandleTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        resultMapper.updateById(r);
        // 确认质控项并生成扣款单 → 联动违规追回
        if (Boolean.TRUE.equals(dto.getCreateRecover())) {
            createRecover(r);
        }
        return ActionVO.ok();
    }

    @Override
    public QualityStatsVO stats() {
        List<MifQualityResult> all = resultMapper.selectList(null);
        QualityStatsVO vo = new QualityStatsVO();
        vo.setHitCount(all.size());
        int tip = 0;
        int warn = 0;
        int serious = 0;
        int pending = 0;
        BigDecimal involve = BigDecimal.ZERO;
        BigDecimal deduct = BigDecimal.ZERO;
        java.util.Set<Long> bills = new java.util.HashSet<>();
        java.util.Set<Long> items = new java.util.HashSet<>();
        for (MifQualityResult r : all) {
            if (r.getSeverity() != null) {
                if (r.getSeverity() == 1) {
                    tip++;
                } else if (r.getSeverity() == 2) {
                    warn++;
                } else {
                    serious++;
                }
            }
            if (r.getHandleStatus() != null && r.getHandleStatus() == 0) {
                pending++;
            }
            involve = involve.add(r.getInvolveAmount() == null ? BigDecimal.ZERO : r.getInvolveAmount());
            deduct = deduct.add(r.getDeductAmount() == null ? BigDecimal.ZERO : r.getDeductAmount());
            if (r.getBillId() != null) {
                bills.add(r.getBillId());
            }
            if (r.getItemId() != null) {
                items.add(r.getItemId());
            }
        }
        vo.setTipCount(tip);
        vo.setWarnCount(warn);
        vo.setSeriousCount(serious);
        vo.setPendingCount(pending);
        vo.setCheckedBills(bills.size());
        vo.setCheckedItems(items.size());
        vo.setInvolveAmount(involve.setScale(2, RoundingMode.HALF_UP));
        vo.setDeductAmount(deduct.setScale(2, RoundingMode.HALF_UP));
        vo.setRuleCount(ruleMapper.selectCount(null).intValue());
        return vo;
    }

    // ------------------------------------------------------------------

    private Map<Long, List<MisFeeItem>> loadItemsByBill() {
        Map<Long, List<MisFeeItem>> map = new HashMap<>();
        for (MisFeeItem it : feeItemMapper.selectList(null)) {
            if (it.getOwnerType() != null && it.getOwnerType() == 2) {
                map.computeIfAbsent(it.getOwnerId(), k -> new java.util.ArrayList<>()).add(it);
            }
        }
        return map;
    }

    /**
     * 命中留痕，按（结算单 + 规则 + 明细 + 说明）幂等去重。
     */
    private boolean saveHit(QualityEngine.Hit h) {
        LambdaQueryWrapper<MifQualityResult> w = new LambdaQueryWrapper<MifQualityResult>()
                .eq(MifQualityResult::getBillId, h.getBillId())
                .eq(MifQualityResult::getRuleCode, h.getRuleCode())
                .eq(MifQualityResult::getItemId, h.getItemId() == null ? 0L : h.getItemId())
                .eq(MifQualityResult::getQualityDesc, h.getQualityDesc() == null ? "" : h.getQualityDesc());
        Long dup = resultMapper.selectCount(w);
        if (dup != null && dup > 0) {
            return false;
        }
        MifQualityResult r = new MifQualityResult();
        r.setBillId(h.getBillId());
        r.setBillNo(h.getBillNo());
        r.setItemId(h.getItemId());
        r.setItemName(h.getItemName());
        r.setYbCode(h.getYbCode());
        r.setMedinsCode(h.getMedinsCode());
        r.setMedinsName(h.getMedinsName());
        r.setRuleId(h.getRuleId());
        r.setRuleCode(h.getRuleCode());
        r.setRuleName(h.getRuleName());
        r.setRuleType(h.getRuleType());
        r.setSeverity(h.getSeverity());
        r.setFieldName(h.getFieldName());
        r.setOriginValue(h.getOriginValue());
        r.setQualityDesc(h.getQualityDesc());
        r.setInvolveAmount(h.getInvolveAmount());
        r.setDeductAmount(h.getDeductAmount());
        r.setHandleStatus(0);
        r.setCheckTime(LocalDateTime.now());
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        resultMapper.insert(r);
        return true;
    }

    /** 质控确认 → 生成违规追回扣款单 */
    private void createRecover(MifQualityResult r) {
        String recoverNo = "QC" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", SEQ.incrementAndGet());
        MifRecover rec = new MifRecover();
        rec.setRecoverNo(recoverNo);
        rec.setSourceType(4);
        rec.setSourceNo(r.getBillNo());
        rec.setTargetOrgCode(r.getMedinsCode());
        rec.setTargetOrgName(r.getMedinsName());
        rec.setMonth(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        rec.setRecoverAmount(r.getDeductAmount() == null ? BigDecimal.ZERO : r.getDeductAmount());
        rec.setReason("结算清单质控扣款：" + r.getRuleName() + " — " + r.getQualityDesc());
        rec.setStatus("待追回");
        rec.setHandler(UserContext.username());
        rec.setRemark("质控规则：" + r.getRuleCode());
        recoverMapper.insert(rec);
    }

    private String buildOpinion(String opinion) {
        String user = UserContext.username();
        String who = user == null || user.isBlank() ? "系统" : user;
        String stamp = LocalDateTime.now().toString().replace('T', ' ').substring(0, 16);
        return "[" + who + " " + stamp + "] " + (opinion == null || opinion.isBlank() ? "已处理" : opinion);
    }

    private QualityRuleVO toRuleVO(MifQualityRule r) {
        QualityRuleVO vo = new QualityRuleVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }

    private QualityResultVO toResultVO(MifQualityResult r) {
        QualityResultVO vo = new QualityResultVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}
