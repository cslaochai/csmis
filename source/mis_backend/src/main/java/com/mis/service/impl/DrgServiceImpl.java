package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.DrgCaseOpDTO;
import com.mis.dto.DrgCaseQueryDTO;
import com.mis.dto.DrgGroupQueryDTO;
import com.mis.dto.DrgGroupUpsertDTO;
import com.mis.engine.DrgEngine;
import com.mis.entity.MisBill;
import com.mis.entity.MifDrgCase;
import com.mis.entity.MifDrgGroup;
import com.mis.mapper.MisBillMapper;
import com.mis.mapper.MifDrgCaseMapper;
import com.mis.mapper.MifDrgGroupMapper;
import com.mis.service.DrgService;
import com.mis.vo.ActionVO;
import com.mis.vo.DrgCaseVO;
import com.mis.vo.DrgGroupVO;
import com.mis.vo.DrgStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DRG/DIP 支付方式监管（分组池维护 + 病例入组 + 偏差超支监管 + 特例单议）
 */
@Service
@RequiredArgsConstructor
public class DrgServiceImpl implements DrgService {

    private final MifDrgGroupMapper groupMapper;
    private final MifDrgCaseMapper caseMapper;
    private final MisBillMapper billMapper;
    private final DrgEngine drgEngine;

    // ------------------------------------------------------------------ 病组/病种池

    @Override
    public PageVO<DrgGroupVO> pageGroup(DrgGroupQueryDTO q) {
        Page<MifDrgGroup> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifDrgGroup> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifDrgGroup::getGroupCode, k).or().like(MifDrgGroup::getGroupName, k));
        }
        if (q.getPayType() != null) {
            w.eq(MifDrgGroup::getPayType, q.getPayType());
        }
        if (q.getStatus() != null) {
            w.eq(MifDrgGroup::getStatus, q.getStatus());
        }
        w.orderByDesc(MifDrgGroup::getId);
        Page<MifDrgGroup> res = groupMapper.selectPage(page, w);
        List<DrgGroupVO> vos = res.getRecords().stream().map(this::toGroupVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsertGroup(DrgGroupUpsertDTO dto) {
        Long dup = groupMapper.selectCount(new LambdaQueryWrapper<MifDrgGroup>()
                .eq(MifDrgGroup::getGroupCode, dto.getGroupCode())
                .ne(dto.getId() != null, MifDrgGroup::getId, dto.getId()));
        if (dup != null && dup > 0) {
            return ActionVO.fail("病组编码已存在");
        }
        MifDrgGroup g;
        if (dto.getId() == null) {
            g = new MifDrgGroup();
        } else {
            g = groupMapper.selectById(dto.getId());
            if (g == null) {
                return ActionVO.fail("病组不存在");
            }
        }
        g.setGroupCode(dto.getGroupCode());
        g.setGroupName(dto.getGroupName());
        g.setPayType(dto.getPayType());
        g.setWeight(dto.getWeight());
        g.setRate(dto.getRate());
        g.setPointValue(dto.getPointValue());
        g.setPointCost(dto.getPointCost());
        g.setAvgCost(dto.getAvgCost());
        g.setCaseCount(dto.getCaseCount());
        g.setStatus(dto.getStatus());
        g.setDescription(dto.getDescription());
        g.setUpdateTime(LocalDateTime.now());
        if (dto.getId() == null) {
            g.setCreateTime(LocalDateTime.now());
            groupMapper.insert(g);
        } else {
            groupMapper.updateById(g);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO changeGroupStatus(Long id, Integer status) {
        MifDrgGroup g = groupMapper.selectById(id);
        if (g == null) {
            return ActionVO.fail("病组不存在");
        }
        g.setStatus(status);
        g.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(g);
        return ActionVO.ok();
    }

    // ------------------------------------------------------------------ 病例入组与监管

    @Override
    public PageVO<DrgCaseVO> pageCase(DrgCaseQueryDTO q) {
        Page<MifDrgCase> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifDrgCase> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifDrgCase::getCaseNo, k).or().like(MifDrgCase::getPatientName, k)
                    .or().like(MifDrgCase::getGroupName, k));
        }
        if (q.getPayType() != null) {
            w.eq(MifDrgCase::getPayType, q.getPayType());
        }
        if (q.getStatus() != null) {
            w.eq(MifDrgCase::getStatus, q.getStatus());
        }
        if (Boolean.TRUE.equals(q.getOverBudget())) {
            w.eq(MifDrgCase::getStatus, 3);
        }
        w.orderByDesc(MifDrgCase::getId);
        Page<MifDrgCase> res = caseMapper.selectPage(page, w);
        List<DrgCaseVO> vos = res.getRecords().stream().map(this::toCaseVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    /**
     * 批量入组：对已结算住院结算单做病组匹配与偏差判定，已入组病例跳过（幂等）。
     */
    @Override
    public ActionVO groupCases() {
        List<MifDrgGroup> groups = groupMapper.selectList(new LambdaQueryWrapper<MifDrgGroup>()
                .eq(MifDrgGroup::getStatus, 1));
        if (groups.isEmpty()) {
            return ActionVO.fail("无可用病组/病种池，请先配置并启用");
        }
        List<MisBill> bills = billMapper.selectList(new LambdaQueryWrapper<MisBill>()
                .eq(MisBill::getSettleType, "住院")
                .ne(MisBill::getStatus, "已退费"));
        if (bills.isEmpty()) {
            return ActionVO.fail("暂无已结算住院病例，无法入组");
        }
        int count = 0;
        for (MisBill b : bills) {
            Long exist = caseMapper.selectCount(new LambdaQueryWrapper<MifDrgCase>()
                    .eq(MifDrgCase::getBillId, b.getId()));
            if (exist != null && exist > 0) {
                continue;
            }
            DrgEngine.Graded g = drgEngine.grade(b, groups);
            MifDrgCase c = new MifDrgCase();
            c.setCaseNo(b.getBillNo());
            c.setBillId(b.getId());
            c.setBillNo(b.getBillNo());
            c.setPatientName(b.getPatientName());
            c.setIdCardMask(b.getIdCardMask());
            c.setInsuranceType(b.getInsuranceType());
            c.setPayType(g.getGroup() == null ? 1 : g.getGroup().getPayType());
            c.setGroupCode(g.getGroup() == null ? null : g.getGroup().getGroupCode());
            c.setGroupName(g.getGroup() == null ? null : g.getGroup().getGroupName());
            c.setWeight(g.getWeight());
            c.setEstCost(g.getEstCost());
            c.setActualCost(g.getActualCost());
            c.setDeviationRate(g.getDeviationRate());
            c.setStatus(g.getStatus());
            c.setDeviationReason(g.getReason());
            c.setCaseDate(b.getSettleTime() == null ? LocalDate.now() : b.getSettleTime().toLocalDate());
            c.setCreateTime(LocalDateTime.now());
            c.setUpdateTime(LocalDateTime.now());
            caseMapper.insert(c);
            count++;
            // 回写病组病例数
            if (g.getGroup() != null) {
                MifDrgGroup gr = groupMapper.selectById(g.getGroup().getId());
                if (gr != null) {
                    gr.setCaseCount((gr.getCaseCount() == null ? 0 : gr.getCaseCount()) + 1);
                    groupMapper.updateById(gr);
                }
            }
        }
        return ActionVO.matched(count);
    }

    @Override
    public ActionVO handleCase(DrgCaseOpDTO dto) {
        MifDrgCase c = caseMapper.selectById(dto.getId());
        if (c == null) {
            return ActionVO.fail("病例不存在");
        }
        if (dto.getAction() != null && dto.getAction() == 4) {
            c.setStatus(4);
        } else if (dto.getAction() != null && dto.getAction() == 1) {
            c.setStatus(1);
        } else {
            return ActionVO.fail("不支持的处置动作");
        }
        String user = UserContext.username();
        String who = user == null || user.isBlank() ? "系统" : user;
        c.setDeviationReason((c.getDeviationReason() == null ? "" : c.getDeviationReason() + "；")
                + "[" + who + " " + LocalDateTime.now().toString().replace('T', ' ').substring(0, 16) + "] "
                + (dto.getOpinion() == null || dto.getOpinion().isBlank() ? "特例单议" : dto.getOpinion()));
        c.setUpdateTime(LocalDateTime.now());
        caseMapper.updateById(c);
        return ActionVO.ok();
    }

    @Override
    public DrgStatsVO stats() {
        List<MifDrgCase> all = caseMapper.selectList(null);
        DrgStatsVO vo = new DrgStatsVO();
        vo.setTotalCases(all.size());
        int warn = 0;
        int over = 0;
        int special = 0;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal est = BigDecimal.ZERO;
        for (MifDrgCase c : all) {
            if (c.getStatus() != null && c.getStatus() == 2) {
                warn++;
            } else if (c.getStatus() != null && c.getStatus() == 3) {
                over++;
            } else if (c.getStatus() != null && c.getStatus() == 4) {
                special++;
            }
            cost = cost.add(c.getActualCost() == null ? BigDecimal.ZERO : c.getActualCost());
            est = est.add(c.getEstCost() == null ? BigDecimal.ZERO : c.getEstCost());
        }
        vo.setWarnCases(warn);
        vo.setOverCases(over);
        vo.setSpecialCases(special);
        vo.setTotalCost(cost.setScale(2, RoundingMode.HALF_UP));
        vo.setTotalEstCost(est.setScale(2, RoundingMode.HALF_UP));
        BigDecimal dev = est.compareTo(BigDecimal.ZERO) > 0
                ? cost.subtract(est).divide(est, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vo.setDeviationPercent(DrgEngine.percent(dev));
        vo.setGroupCount(groupMapper.selectCount(null).intValue());
        return vo;
    }

    // ------------------------------------------------------------------

    private DrgGroupVO toGroupVO(MifDrgGroup g) {
        DrgGroupVO vo = new DrgGroupVO();
        BeanUtils.copyProperties(g, vo);
        return vo;
    }

    private DrgCaseVO toCaseVO(MifDrgCase c) {
        DrgCaseVO vo = new DrgCaseVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
