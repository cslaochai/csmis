package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.MisProperties;
import com.mis.config.UserContext;
import com.mis.dto.SuspicionHandleDTO;
import com.mis.dto.SuspicionQueryDTO;
import com.mis.engine.SuperviseRuleEngine;
import com.mis.entity.MifRecover;
import com.mis.entity.MifSuspicion;
import com.mis.mapper.MifRecoverMapper;
import com.mis.mapper.MifSuspicionMapper;
import com.mis.service.AuditRuleService;
import com.mis.service.SuspicionService;
import com.mis.vo.ActionVO;
import com.mis.vo.SuspicionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import com.mis.service.impl.TodoServiceImpl;

@Service
@RequiredArgsConstructor
public class SuspicionServiceImpl implements SuspicionService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MifSuspicionMapper suspicionMapper;
    private final MifRecoverMapper recoverMapper;
    private final AuditRuleService ruleService;
    private final SuperviseRuleEngine ruleEngine;
    private final MisProperties properties;

    @Override
    public PageVO<SuspicionVO> suspicionPage(SuspicionQueryDTO q) {
        Page<MifSuspicion> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifSuspicion> w = new LambdaQueryWrapper<>();
        if (hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(MifSuspicion::getSuspicionNo, kw)
                    .or().like(MifSuspicion::getPatientName, kw)
                    .or().like(MifSuspicion::getBizNo, kw));
        }
        if (hasText(q.getRuleType())) {
            w.eq(MifSuspicion::getRuleType, q.getRuleType());
        }
        if (hasText(q.getStatus())) {
            w.eq(MifSuspicion::getStatus, q.getStatus());
        }
        if (q.getBizSource() != null) {
            w.eq(MifSuspicion::getBizSource, q.getBizSource());
        }
        w.orderByDesc(MifSuspicion::getId);
        Page<MifSuspicion> res = suspicionMapper.selectPage(page, w);
        List<SuspicionVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO runAudit() {
        List<SuperviseRuleEngine.Draft> drafts = ruleEngine.scan(ruleService.enabledRules());
        int inserted = 0;
        for (SuperviseRuleEngine.Draft d : drafts) {
            Long exists = suspicionMapper.selectCount(new LambdaQueryWrapper<MifSuspicion>()
                    .eq(MifSuspicion::getBizSource, d.getBizSource())
                    .eq(MifSuspicion::getBizId, d.getBizId())
                    .eq(MifSuspicion::getRuleId, d.getRuleId()));
            if (exists != null && exists > 0) {
                continue;
            }
            MifSuspicion s = new MifSuspicion();
            s.setSuspicionNo(nextNo("YZ", suspicionMapper.selectCount(new LambdaQueryWrapper<MifSuspicion>()
                    .ge(MifSuspicion::getCreateTime, LocalDate.now().atStartOfDay()))));
            s.setRuleId(d.getRuleId());
            s.setRuleName(d.getRuleName());
            s.setRuleType(d.getRuleType());
            s.setBizSource(d.getBizSource());
            s.setBizId(d.getBizId());
            s.setBizNo(d.getBizNo());
            s.setPatientName(d.getPatientName());
            s.setIdCardMask(d.getIdCardMask());
            s.setInsuranceType(d.getInsuranceType());
            s.setEncounterType(d.getEncounterType());
            s.setDept(d.getDept());
            s.setDoctor(d.getDoctor());
            s.setBizDate(d.getBizDate());
            s.setInvolvedAmount(d.getInvolvedAmount());
            s.setReason(d.getReason());
            s.setStatus("待核实");
            suspicionMapper.insert(s);
            inserted++;

            // 待办中心推送：疑点稽核待核实
            TodoServiceImpl.pushTodo(
                    "mif_suspicion",
                    "疑点稽核待核实：" + s.getSuspicionNo() + " " + d.getReason(),
                    "mif_suspicion", s.getId(), s.getSuspicionNo(),
                    "审核员B", 3L, d.getRuleType() != null && d.getRuleType().contains("高") ? 1 : 2);
        }
        return ActionVO.matched(inserted);
    }

    @Override
    public SuspicionVO getDetail(Long id) {
        MifSuspicion s = suspicionMapper.selectById(id);
        return s == null ? null : toVO(s);
    }

    private SuspicionVO toVO(MifSuspicion s) {
        SuspicionVO vo = new SuspicionVO();
        BeanUtils.copyProperties(s, vo);
        return vo;
    }

    @Override
    public ActionVO confirm(SuspicionHandleDTO dto) {
        MifSuspicion s = suspicionMapper.selectById(dto.getId());
        if (s == null) {
            return ActionVO.fail("疑点不存在");
        }
        if (!"待核实".equals(s.getStatus())) {
            return ActionVO.fail("该疑点已处置，不可重复确认");
        }
        BigDecimal amount = dto.getAmount() != null ? dto.getAmount() : s.getInvolvedAmount();
        s.setStatus("已确认");
        s.setConfirmAmount(amount);
        s.setHandleUser(UserContext.username());
        s.setHandleTime(LocalDateTime.now());
        s.setRemark(dto.getRemark());
        suspicionMapper.updateById(s);

        // 自动生成追回单（来源：疑点稽核）
        MifRecover r = new MifRecover();
        r.setRecoverNo(nextNo("ZJ", recoverMapper.selectCount(new LambdaQueryWrapper<MifRecover>()
                .ge(MifRecover::getCreateTime, LocalDate.now().atStartOfDay()))));
        r.setSourceType(1);
        r.setSourceNo(s.getSuspicionNo());
        r.setTargetOrgCode(properties.getMedins().getCode());
        r.setTargetOrgName(properties.getMedins().getName());
        r.setMonth(s.getBizDate() == null ? null : YearMonth.from(s.getBizDate()).toString());
        r.setRecoverAmount(amount);
        r.setReason(cut(s.getReason(), 500));
        r.setStatus("待追回");
        recoverMapper.insert(r);
        return ActionVO.ok();
    }

    @Override
    public ActionVO exclude(SuspicionHandleDTO dto) {
        MifSuspicion s = suspicionMapper.selectById(dto.getId());
        if (s == null) {
            return ActionVO.fail("疑点不存在");
        }
        if (!"待核实".equals(s.getStatus())) {
            return ActionVO.fail("该疑点已处置，不可重复排除");
        }
        s.setStatus("已排除");
        s.setHandleUser(UserContext.username());
        s.setHandleTime(LocalDateTime.now());
        s.setRemark(dto.getRemark());
        suspicionMapper.updateById(s);
        return ActionVO.ok();
    }

    private String nextNo(String prefix, long todayCount) {
        return prefix + LocalDate.now().format(DAY) + String.format("%03d", todayCount + 1);
    }

    private static String cut(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
