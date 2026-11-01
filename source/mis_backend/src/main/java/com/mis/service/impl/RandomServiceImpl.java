package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.RandomResultHandleDTO;
import com.mis.dto.RandomResultQueryDTO;
import com.mis.dto.RandomTaskQueryDTO;
import com.mis.dto.RandomTaskUpsertDTO;
import com.mis.entity.MisAgreement;
import com.mis.entity.SysEmployee;
import com.mis.entity.MifRandomResult;
import com.mis.entity.MifRandomTask;
import com.mis.mapper.MisAgreementMapper;
import com.mis.mapper.SysEmployeeMapper;
import com.mis.mapper.MifRandomResultMapper;
import com.mis.mapper.MifRandomTaskMapper;
import com.mis.service.RandomService;
import com.mis.vo.ActionVO;
import com.mis.vo.RandomResultVO;
import com.mis.vo.RandomTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 双随机一公开：随机抽取检查对象（定点机构）与检查人员，检查结果对外公开。
 */
@Service
@RequiredArgsConstructor
public class RandomServiceImpl implements RandomService {

    private final MifRandomTaskMapper taskMapper;
    private final MifRandomResultMapper resultMapper;
    private final MisAgreementMapper agreementMapper;
    private final SysEmployeeMapper employeeMapper;

    private static final java.util.concurrent.atomic.AtomicLong SEQ =
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() % 100000);

    // ------------------------------------------------------------------ 任务

    @Override
    public PageVO<RandomTaskVO> pageTask(RandomTaskQueryDTO q) {
        Page<MifRandomTask> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifRandomTask> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifRandomTask::getTaskNo, k).or().like(MifRandomTask::getTaskName, k));
        }
        if (q.getCheckType() != null) {
            w.eq(MifRandomTask::getCheckType, q.getCheckType());
        }
        if (q.getStatus() != null) {
            w.eq(MifRandomTask::getStatus, q.getStatus());
        }
        w.orderByDesc(MifRandomTask::getId);
        Page<MifRandomTask> res = taskMapper.selectPage(page, w);
        List<RandomTaskVO> vos = res.getRecords().stream().map(this::toTaskVO).collect(Collectors.toList());
        for (RandomTaskVO vo : vos) {
            vo.setDrawCount(resultMapper.selectCount(new LambdaQueryWrapper<MifRandomResult>()
                    .eq(MifRandomResult::getTaskId, vo.getId())
                    .eq(MifRandomResult::getTargetType, 1)).intValue());
            vo.setCheckerCount(resultMapper.selectCount(new LambdaQueryWrapper<MifRandomResult>()
                    .eq(MifRandomResult::getTaskId, vo.getId())
                    .eq(MifRandomResult::getTargetType, 2)).intValue());
        }
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsertTask(RandomTaskUpsertDTO dto) {
        MifRandomTask t;
        if (dto.getId() == null) {
            t = new MifRandomTask();
            t.setTaskNo("SR" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%04d", SEQ.incrementAndGet()));
            t.setStatus(1);
        } else {
            t = taskMapper.selectById(dto.getId());
            if (t == null) {
                return ActionVO.fail("抽查任务不存在");
            }
        }
        t.setTaskName(dto.getTaskName());
        t.setCheckType(dto.getCheckType());
        t.setPlanCount(dto.getPlanCount());
        t.setPlanCheckerCount(dto.getPlanCheckerCount());
        t.setDeptName(dto.getDeptName());
        t.setRemark(dto.getRemark());
        t.setUpdateTime(LocalDateTime.now());
        if (dto.getId() == null) {
            t.setCreateTime(LocalDateTime.now());
            taskMapper.insert(t);
        } else {
            taskMapper.updateById(t);
        }
        return ActionVO.ok();
    }

    /**
     * 双随机抽取：从已签订协议的定点机构中随机抽 planCount 个作为检查对象，
     * 从在职人员中随机抽 planCheckerCount 个作为检查人员；抽取过程与结果全部留痕。
     */
    @Override
    public ActionVO draw(Long taskId) {
        MifRandomTask t = taskMapper.selectById(taskId);
        if (t == null) {
            return ActionVO.fail("抽查任务不存在");
        }
        if (t.getStatus() != null && t.getStatus() == 3) {
            return ActionVO.fail("该任务已完成，不能重复抽取");
        }
        List<MisAgreement> medins = agreementMapper.selectList(new LambdaQueryWrapper<MisAgreement>()
                .eq(MisAgreement::getAgreementStatus, 1));
        if (medins.isEmpty()) {
            return ActionVO.fail("无有效协议定点机构，无法抽取检查对象");
        }
        List<SysEmployee> checkers = employeeMapper.selectList(new LambdaQueryWrapper<SysEmployee>()
                .eq(SysEmployee::getStatus, 1));
        if (checkers.isEmpty()) {
            return ActionVO.fail("无可用检查人员，无法抽取");
        }
        // 清除上一次未完成的抽取结果，保证一次抽取对应一组名单
        resultMapper.delete(new LambdaQueryWrapper<MifRandomResult>().eq(MifRandomResult::getTaskId, taskId));

        int planCount = t.getPlanCount() == null ? 0 : t.getPlanCount();
        int planChecker = t.getPlanCheckerCount() == null ? 0 : t.getPlanCheckerCount();
        List<MisAgreement> pickedMedins = randomPick(medins, Math.min(planCount, medins.size()));
        List<SysEmployee> pickedCheckers = randomPick(checkers, Math.min(planChecker, checkers.size()));
        if (pickedMedins.isEmpty()) {
            return ActionVO.fail("抽取失败：可抽取的检查对象不足");
        }
        LocalDateTime now = LocalDateTime.now();
        for (MisAgreement m : pickedMedins) {
            MifRandomResult r = new MifRandomResult();
            r.setTaskId(taskId);
            r.setTargetType(1);
            r.setTargetCode(m.getMedinsCode());
            r.setTargetName(m.getMedinsName());
            r.setOrgName(m.getMedinsName());
            r.setDrawTime(now);
            r.setResult(1);
            r.setPublicFlag(0);
            r.setCreateTime(now);
            r.setUpdateTime(now);
            resultMapper.insert(r);
        }
        for (SysEmployee e : pickedCheckers) {
            MifRandomResult r = new MifRandomResult();
            r.setTaskId(taskId);
            r.setTargetType(2);
            r.setTargetCode(e.getEmpCode());
            r.setTargetName(e.getEmpName());
            r.setOrgName(e.getDeptName());
            r.setDrawTime(now);
            r.setResult(1);
            r.setPublicFlag(0);
            r.setCreateTime(now);
            r.setUpdateTime(now);
            resultMapper.insert(r);
        }
        t.setDrawTime(now);
        t.setStatus(2);
        t.setUpdateTime(now);
        taskMapper.updateById(t);
        return ActionVO.matched(pickedMedins.size() + pickedCheckers.size());
    }

    /** 结果公开：把该任务下已完成的检查结果标记为已公开 */
    @Override
    public ActionVO publish(Long taskId) {
        MifRandomTask t = taskMapper.selectById(taskId);
        if (t == null) {
            return ActionVO.fail("抽查任务不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        List<MifRandomResult> results = resultMapper.selectList(new LambdaQueryWrapper<MifRandomResult>()
                .eq(MifRandomResult::getTaskId, taskId)
                .ne(MifRandomResult::getResult, 1));
        for (MifRandomResult r : results) {
            r.setPublicFlag(1);
            r.setPublicTime(now);
            r.setUpdateTime(now);
            resultMapper.updateById(r);
        }
        t.setStatus(3);
        t.setUpdateTime(now);
        taskMapper.updateById(t);
        return ActionVO.matched(results.size());
    }

    // ------------------------------------------------------------------ 抽取结果

    @Override
    public PageVO<RandomResultVO> pageResult(RandomResultQueryDTO q) {
        Page<MifRandomResult> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifRandomResult> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifRandomResult::getTargetName, k).or().like(MifRandomResult::getTargetCode, k)
                    .or().like(MifRandomResult::getOrgName, k));
        }
        if (q.getTaskId() != null) {
            w.eq(MifRandomResult::getTaskId, q.getTaskId());
        }
        if (q.getTargetType() != null) {
            w.eq(MifRandomResult::getTargetType, q.getTargetType());
        }
        if (q.getResult() != null) {
            w.eq(MifRandomResult::getResult, q.getResult());
        }
        if (q.getPublicFlag() != null) {
            w.eq(MifRandomResult::getPublicFlag, q.getPublicFlag());
        }
        w.orderByDesc(MifRandomResult::getId);
        Page<MifRandomResult> res = resultMapper.selectPage(page, w);
        List<RandomResultVO> vos = res.getRecords().stream().map(this::toResultVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO handleResult(RandomResultHandleDTO dto) {
        MifRandomResult r = resultMapper.selectById(dto.getId());
        if (r == null) {
            return ActionVO.fail("抽取结果不存在");
        }
        r.setResult(dto.getResult());
        r.setCheckResult(dto.getCheckResult());
        if (dto.getPublicFlag() != null) {
            r.setPublicFlag(dto.getPublicFlag());
            if (dto.getPublicFlag() == 1 && r.getPublicTime() == null) {
                r.setPublicTime(LocalDateTime.now());
            }
        }
        r.setUpdateTime(LocalDateTime.now());
        resultMapper.updateById(r);
        return ActionVO.ok();
    }

    // ------------------------------------------------------------------

    /** 无放回随机抽样 */
    private <T> List<T> randomPick(List<T> source, int n) {
        List<T> copy = new ArrayList<>(source);
        Collections.shuffle(copy, ThreadLocalRandom.current());
        return new ArrayList<>(copy.subList(0, Math.max(0, Math.min(n, copy.size()))));
    }

    private RandomTaskVO toTaskVO(MifRandomTask t) {
        RandomTaskVO vo = new RandomTaskVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }

    private RandomResultVO toResultVO(MifRandomResult r) {
        RandomResultVO vo = new RandomResultVO();
        BeanUtils.copyProperties(r, vo);
        MifRandomTask t = taskMapper.selectById(r.getTaskId());
        vo.setTaskName(t == null ? null : t.getTaskName());
        return vo;
    }
}
