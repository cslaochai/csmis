package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.FraudClueHandleDTO;
import com.mis.dto.FraudClueQueryDTO;
import com.mis.dto.FraudModelQueryDTO;
import com.mis.dto.FraudModelStatusDTO;
import com.mis.dto.FraudModelUpsertDTO;
import com.mis.engine.FraudEngine;
import com.mis.entity.MisInsuredPerson;
import com.mis.entity.MisAgreement;
import com.mis.entity.MifFraudClue;
import com.mis.entity.MifFraudModel;
import com.mis.entity.MifRecover;
import com.mis.mapper.MisInsuredPersonMapper;
import com.mis.mapper.MisAgreementMapper;
import com.mis.mapper.MifFraudClueMapper;
import com.mis.mapper.MifFraudModelMapper;
import com.mis.mapper.MifRecoverMapper;
import com.mis.service.FraudService;
import com.mis.vo.ActionVO;
import com.mis.vo.FraudClueVO;
import com.mis.vo.FraudModelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.mis.service.impl.TodoServiceImpl;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudServiceImpl implements FraudService {

    private final MifFraudModelMapper modelMapper;
    private final MifFraudClueMapper clueMapper;
    private final MisAgreementMapper agreementMapper;
    private final MisInsuredPersonMapper personMapper;
    private final FraudEngine fraudEngine;
    private final MifRecoverMapper recoverMapper;

    private static final AtomicLong CLUE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);
    private static final AtomicLong RECOVER_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Override
    public PageVO<FraudModelVO> pageModel(FraudModelQueryDTO q) {
        Page<MifFraudModel> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifFraudModel> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifFraudModel::getModelCode, k).or().like(MifFraudModel::getModelName, k));
        }
        if (StringUtils.hasText(q.getCategory())) {
            w.eq(MifFraudModel::getCategory, q.getCategory());
        }
        if (q.getStatus() != null) {
            w.eq(MifFraudModel::getStatus, q.getStatus());
        }
        w.orderByDesc(MifFraudModel::getId);
        Page<MifFraudModel> res = modelMapper.selectPage(page, w);
        List<FraudModelVO> vos = res.getRecords().stream().map(this::toModelVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsertModel(FraudModelUpsertDTO dto) {
        Long dup = modelMapper.selectCount(new LambdaQueryWrapper<MifFraudModel>()
                .eq(MifFraudModel::getModelCode, dto.getModelCode())
                .ne(dto.getId() != null, MifFraudModel::getId, dto.getId()));
        if (dup != null && dup > 0) {
            return ActionVO.fail("模型编码已存在");
        }
        MifFraudModel m;
        if (dto.getId() == null) {
            m = new MifFraudModel();
        } else {
            m = modelMapper.selectById(dto.getId());
            if (m == null) {
                return ActionVO.fail("模型不存在");
            }
        }
        m.setModelCode(dto.getModelCode());
        m.setModelName(dto.getModelName());
        m.setCategory(dto.getCategory());
        m.setRiskLevel(dto.getRiskLevel());
        m.setAlgorithm(dto.getAlgorithm());
        m.setParams(dto.getParams());
        m.setStatus(dto.getStatus());
        m.setDescription(dto.getDescription());
        m.setUpdateTime(LocalDateTime.now());
        if (dto.getId() == null) {
            m.setCreateTime(LocalDateTime.now());
            modelMapper.insert(m);
        } else {
            modelMapper.updateById(m);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO changeModelStatus(FraudModelStatusDTO dto) {
        MifFraudModel m = modelMapper.selectById(dto.getId());
        if (m == null) {
            return ActionVO.fail("模型不存在");
        }
        m.setStatus(dto.getStatus());
        m.setUpdateTime(LocalDateTime.now());
        modelMapper.updateById(m);
        return ActionVO.ok();
    }

    @Override
    public PageVO<FraudClueVO> pageClue(FraudClueQueryDTO q) {
        Page<MifFraudClue> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifFraudClue> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifFraudClue::getClueNo, k).or().like(MifFraudClue::getMedinsName, k)
                    .or().like(MifFraudClue::getInsuredName, k).or().like(MifFraudClue::getInsuredIdCard, k));
        }
        if (q.getStatus() != null) {
            w.eq(MifFraudClue::getStatus, q.getStatus());
        }
        if (q.getRiskLevel() != null) {
            w.eq(MifFraudClue::getRiskLevel, q.getRiskLevel());
        }
        w.orderByDesc(MifFraudClue::getId);
        Page<MifFraudClue> res = clueMapper.selectPage(page, w);
        List<FraudClueVO> vos = res.getRecords().stream().map(this::toClueVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    /**
     * 真实模型运行：对业务数据执行各启用模型，命中即生成线索（按 模型+业务单号 幂等去重）。
     */
    @Override
    public ActionVO run(Long modelId) {
        LambdaQueryWrapper<MifFraudModel> mw = new LambdaQueryWrapper<>();
        if (modelId != null) {
            mw.eq(MifFraudModel::getId, modelId);
        } else {
            mw.eq(MifFraudModel::getStatus, 1);
        }
        List<MifFraudModel> models = modelMapper.selectList(mw);
        if (models.isEmpty()) {
            return ActionVO.fail("无可用模型（请先启用模型）");
        }
        int count = 0;
        for (MifFraudModel model : models) {
            List<FraudEngine.Clue> clues;
            try {
                clues = fraudEngine.run(model);
            } catch (Exception e) {
                log.warn("[反欺诈] 模型 {} 执行失败: {}", model.getModelCode(), e.getMessage());
                continue;
            }
            for (FraudEngine.Clue c : clues) {
                if (saveClue(c)) {
                    count++;
                }
            }
        }
        return ActionVO.matched(count);
    }

    @Override
    public ActionVO handleClue(FraudClueHandleDTO dto) {
        MifFraudClue c = clueMapper.selectById(dto.getId());
        if (c == null) {
            return ActionVO.fail("线索不存在");
        }
        if (c.getStatus() != null && c.getStatus() != 1) {
            return ActionVO.fail("该线索已处置，不能重复操作");
        }
        c.setStatus(dto.getStatus());
        c.setHandleUser(dto.getHandleUser());
        c.setHandleOpinion(dto.getHandleOpinion());
        c.setHandleTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        clueMapper.updateById(c);
        // 结果应用闭环：移送线索自动生成违规追回单
        if (dto.getStatus() != null && dto.getStatus() == 4) {
            createRecover(c);
        }
        return ActionVO.ok();
    }

    /** 移送反欺诈线索 → 生成违规追回单（待追回） */
    private void createRecover(MifFraudClue c) {
        String recoverNo = "RC" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", RECOVER_SEQ.incrementAndGet());
        MifRecover r = new MifRecover();
        r.setRecoverNo(recoverNo);
        r.setSourceType(3);
        r.setSourceNo(c.getClueNo());
        r.setTargetOrgCode(c.getMedinsCode());
        r.setTargetOrgName(c.getMedinsName());
        r.setMonth(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        r.setRecoverAmount(c.getInvolveAmount() == null ? BigDecimal.ZERO : c.getInvolveAmount());
        r.setReason("反欺诈线索移送：" + (c.getClueDetail() == null ? "" : c.getClueDetail()));
        r.setStatus("待追回");
        r.setHandler(UserContext.username());
        r.setRemark("来源模型：" + c.getModelName() + "；涉及业务单号：" + c.getBizNo());
        recoverMapper.insert(r);
    }

    /**
     * 生成线索并落库，按（模型 + 业务单号 + 线索详情）幂等去重。
     */
    private boolean saveClue(FraudEngine.Clue c) {
        if (c.getBizNo() == null || c.getBizNo().isBlank()) {
            return false;
        }
        Long dup = clueMapper.selectCount(new LambdaQueryWrapper<MifFraudClue>()
                .eq(MifFraudClue::getModelId, c.getModelId())
                .eq(MifFraudClue::getBizNo, c.getBizNo())
                .eq(MifFraudClue::getClueDetail, c.getClueDetail() == null ? "" : c.getClueDetail()));
        if (dup != null && dup > 0) {
            return false;
        }
        MifFraudClue clue = new MifFraudClue();
        clue.setClueNo(genClueNo());
        clue.setModelId(c.getModelId());
        clue.setModelCode(c.getModelCode());
        clue.setModelName(c.getModelName());
        clue.setMedinsCode(c.getMedinsCode());
        clue.setMedinsName(c.getMedinsName());
        clue.setInsuredIdCard(c.getInsuredIdCard());
        clue.setInsuredName(c.getInsuredName());
        clue.setBizNo(c.getBizNo());
        clue.setInvolveAmount(c.getInvolveAmount());
        clue.setRiskLevel(c.getRiskLevel());
        clue.setClueDetail(c.getClueDetail());
        clue.setStatus(1);
        clue.setCreateTime(LocalDateTime.now());
        clue.setUpdateTime(LocalDateTime.now());
        clueMapper.insert(clue);

        // 待办中心推送：反欺诈线索待核查（风险等级越高优先级越高）
        TodoServiceImpl.pushTodo(
                "mif_fraud_clue",
                "反欺诈线索待核查：" + clue.getClueNo() + " " + c.getClueDetail(),
                "mif_fraud_clue", clue.getId(), clue.getClueNo(),
                "审核员B", 3L, c.getRiskLevel() != null && c.getRiskLevel() >= 3 ? 1 : 2);
        return true;
    }

    /** 线索编号：CL + yyyyMMdd + 6 位序号 */
    private String genClueNo() {
        return "CL" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%06d", CLUE_SEQ.incrementAndGet());
    }

    private FraudModelVO toModelVO(MifFraudModel m) {
        FraudModelVO vo = new FraudModelVO();
        BeanUtils.copyProperties(m, vo);
        return vo;
    }

    private FraudClueVO toClueVO(MifFraudClue c) {
        FraudClueVO vo = new FraudClueVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
