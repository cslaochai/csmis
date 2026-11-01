package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.TraceCodeQueryDTO;
import com.mis.dto.TraceCodeUpsertDTO;
import com.mis.dto.TraceLogQueryDTO;
import com.mis.dto.TraceVerifyDTO;
import com.mis.entity.MisBill;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MifTraceCode;
import com.mis.entity.MifTraceVerifyLog;
import com.mis.mapper.MisBillMapper;
import com.mis.mapper.MisFeeItemMapper;
import com.mis.mapper.MifTraceCodeMapper;
import com.mis.mapper.MifTraceVerifyLogMapper;
import com.mis.service.TraceService;
import com.mis.vo.ActionVO;
import com.mis.vo.TraceCodeVO;
import com.mis.vo.TraceLogVO;
import com.mis.vo.TraceStatsVO;
import com.mis.vo.TraceVerifyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 药品追溯码监管：台账登记 → 结算扫码核验 → 日志留痕 → 异常识别
 */
@Service
@RequiredArgsConstructor
public class TraceServiceImpl implements TraceService {

    private final MifTraceCodeMapper codeMapper;
    private final MifTraceVerifyLogMapper logMapper;
    private final MisBillMapper billMapper;
    private final MisFeeItemMapper feeItemMapper;

    /** 需采集追溯码的项目类别 */
    private static final List<String> DRUG_CATEGORIES = List.of("西药", "中成药", "中草药");

    // ------------------------------------------------------------------ 台账

    @Override
    public PageVO<TraceCodeVO> pageCode(TraceCodeQueryDTO q) {
        Page<MifTraceCode> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifTraceCode> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifTraceCode::getCode, k).or().like(MifTraceCode::getDrugName, k)
                    .or().like(MifTraceCode::getBatchNo, k).or().like(MifTraceCode::getBillNo, k));
        }
        if (StringUtils.hasText(q.getDrugName())) {
            w.like(MifTraceCode::getDrugName, q.getDrugName());
        }
        if (q.getStatus() != null) {
            w.eq(MifTraceCode::getStatus, q.getStatus());
        }
        if (StringUtils.hasText(q.getBillNo())) {
            w.eq(MifTraceCode::getBillNo, q.getBillNo());
        }
        w.orderByDesc(MifTraceCode::getId);
        Page<MifTraceCode> res = codeMapper.selectPage(page, w);
        List<TraceCodeVO> vos = res.getRecords().stream().map(this::toCodeVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO register(TraceCodeUpsertDTO dto) {
        List<TraceCodeUpsertDTO> list = new ArrayList<>();
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            list.addAll(dto.getItems());
        } else if (dto.getCode() != null && !dto.getCode().isBlank()) {
            list.add(dto);
        }
        if (list.isEmpty()) {
            return ActionVO.fail("未提供追溯码");
        }
        int added = 0;
        int skipped = 0;
        LocalDateTime now = LocalDateTime.now();
        for (TraceCodeUpsertDTO d : list) {
            if (d.getCode() == null || d.getCode().isBlank()) {
                skipped++;
                continue;
            }
            Long dup = codeMapper.selectCount(new LambdaQueryWrapper<MifTraceCode>()
                    .eq(MifTraceCode::getCode, d.getCode().trim()));
            if (dup != null && dup > 0) {
                skipped++;
                continue;
            }
            MifTraceCode c = new MifTraceCode();
            c.setCode(d.getCode().trim());
            c.setDrugName(d.getDrugName());
            c.setSpec(d.getSpec());
            c.setManufacturer(d.getManufacturer());
            c.setBatchNo(d.getBatchNo());
            c.setExpireDate(d.getExpireDate());
            c.setStatus(1);
            c.setVerifyCount(0);
            c.setCreateTime(now);
            codeMapper.insert(c);
            added++;
        }
        ActionVO vo = ActionVO.matched(added);
        vo.setError(skipped > 0 ? "重复或无效码已跳过 " + skipped + " 条" : null);
        return vo;
    }

    // ------------------------------------------------------------------ 核验

    @Override
    public TraceVerifyVO verify(TraceVerifyDTO dto) {
        MifTraceCode c = codeMapper.selectOne(new LambdaQueryWrapper<MifTraceCode>()
                .eq(MifTraceCode::getCode, dto.getCode() == null ? "" : dto.getCode().trim())
                .last("limit 1"));
        TraceVerifyVO vo = new TraceVerifyVO();
        vo.setCode(dto.getCode());
        vo.setBillNo(dto.getBillNo());
        if (c == null) {
            vo.setVerifyResult(2);
            vo.setVerifyDesc("追溯码 " + dto.getCode() + " 未在台账中登记，疑似串换药品或未采集追溯码");
            vo.setConsumed(false);
            writeLog(dto.getCode(), dto.getBillNo(), dto.getItemName(), null, 2, vo.getVerifyDesc());
            return vo;
        }
        vo.setDrugName(c.getDrugName());
        LocalDate bizDate = dto.getBizDate() != null ? dto.getBizDate() : LocalDate.now();
        int result = judge(c, dto.getItemName(), bizDate);
        vo.setVerifyResult(result);
        vo.setVerifyDesc(descOf(result, c, dto.getItemName()));
        vo.setConsumed(c.getStatus() != null && c.getStatus() == 2);
        // 核验通过且尚未核销 → 核销该码
        if (result == 1 && c.getStatus() != null && c.getStatus() == 1) {
            c.setStatus(2);
            c.setBillNo(dto.getBillNo());
            c.setItemName(dto.getItemName());
            c.setScanUser(UserContext.username());
            c.setScanTime(LocalDateTime.now());
            c.setVerifyCount((c.getVerifyCount() == null ? 0 : c.getVerifyCount()) + 1);
            codeMapper.updateById(c);
        } else {
            c.setVerifyCount((c.getVerifyCount() == null ? 0 : c.getVerifyCount()) + 1);
            codeMapper.updateById(c);
        }
        writeLog(dto.getCode(), dto.getBillNo(), dto.getItemName(), c.getDrugName(), result, vo.getVerifyDesc());
        return vo;
    }

    /**
     * 对结算单批量核验：对药品类明细逐条匹配追溯码台账，核验通过即核销。
     */
    @Override
    public ActionVO verifyBill(String billNo) {
        MisBill bill = billMapper.selectOne(new LambdaQueryWrapper<MisBill>()
                .eq(MisBill::getBillNo, billNo)
                .last("limit 1"));
        if (bill == null) {
            return ActionVO.fail("结算单不存在");
        }
        List<MisFeeItem> items = feeItemMapper.selectList(new LambdaQueryWrapper<MisFeeItem>()
                .eq(MisFeeItem::getOwnerType, 2)
                .eq(MisFeeItem::getOwnerId, bill.getId())
                .orderByAsc(MisFeeItem::getId));
        if (items.isEmpty()) {
            return ActionVO.fail("该结算单无费用明细，无法核验追溯码");
        }
        LocalDate bizDate = bill.getSettleTime() == null ? LocalDate.now() : bill.getSettleTime().toLocalDate();
        List<MifTraceCode> codes = codeMapper.selectList(null);
        int checked = 0;
        int abnormal = 0;
        for (MisFeeItem it : items) {
            if (it.getCategory() == null || !DRUG_CATEGORIES.contains(it.getCategory())) {
                continue;
            }
            checked++;
            // 按药品名称匹配未使用码；取到则核销，取不到则按「不存在」处理
            MifTraceCode matched = null;
            for (MifTraceCode c : codes) {
                if (c.getStatus() != null && c.getStatus() == 1
                        && c.getDrugName() != null && it.getName() != null
                        && c.getDrugName().equals(it.getName())) {
                    matched = c;
                    break;
                }
            }
            if (matched == null) {
                abnormal++;
                writeLog("", bill.getBillNo(), it.getName(), null, 2,
                        "项目「" + it.getName() + "」未采集到追溯码，金额 " + nz(it.getAmount()) + " 元");
                continue;
            }
            int result = judge(matched, it.getName(), bizDate);
            if (result == 1) {
                matched.setStatus(2);
                matched.setBillId(bill.getId());
                matched.setBillNo(bill.getBillNo());
                matched.setItemId(it.getId());
                matched.setItemName(it.getName());
                matched.setScanUser(UserContext.username());
                matched.setScanTime(LocalDateTime.now());
                matched.setVerifyCount((matched.getVerifyCount() == null ? 0 : matched.getVerifyCount()) + 1);
                codeMapper.updateById(matched);
            } else {
                abnormal++;
                matched.setVerifyCount((matched.getVerifyCount() == null ? 0 : matched.getVerifyCount()) + 1);
                codeMapper.updateById(matched);
            }
            writeLog(matched.getCode(), bill.getBillNo(), it.getName(), matched.getDrugName(), result,
                    descOf(result, matched, it.getName()));
        }
        if (checked == 0) {
            return ActionVO.fail("该结算单无药品类明细，无需核验追溯码");
        }
        return ActionVO.matched(checked);
    }

    // ------------------------------------------------------------------ 日志与统计

    @Override
    public PageVO<TraceLogVO> pageLog(TraceLogQueryDTO q) {
        Page<MifTraceVerifyLog> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MifTraceVerifyLog> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MifTraceVerifyLog::getCode, k).or().like(MifTraceVerifyLog::getItemName, k)
                    .or().like(MifTraceVerifyLog::getBillNo, k).or().like(MifTraceVerifyLog::getProblemDesc, k));
        }
        if (q.getVerifyResult() != null) {
            w.eq(MifTraceVerifyLog::getVerifyResult, q.getVerifyResult());
        }
        if (StringUtils.hasText(q.getBillNo())) {
            w.eq(MifTraceVerifyLog::getBillNo, q.getBillNo());
        }
        w.orderByDesc(MifTraceVerifyLog::getId);
        Page<MifTraceVerifyLog> res = logMapper.selectPage(page, w);
        List<TraceLogVO> vos = res.getRecords().stream().map(this::toLogVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public TraceStatsVO stats() {
        List<MifTraceCode> codes = codeMapper.selectList(null);
        TraceStatsVO vo = new TraceStatsVO();
        vo.setTotalCodes(codes.size());
        int unused = 0;
        int used = 0;
        int voided = 0;
        for (MifTraceCode c : codes) {
            if (c.getStatus() == null || c.getStatus() == 1) {
                unused++;
            } else if (c.getStatus() == 2) {
                used++;
            } else {
                voided++;
            }
        }
        vo.setUnusedCodes(unused);
        vo.setUsedCodes(used);
        vo.setVoidCodes(voided);
        List<MifTraceVerifyLog> logs = logMapper.selectList(null);
        vo.setVerifyCount(logs.size());
        int pass = 0;
        int missing = 0;
        int reused = 0;
        int mismatch = 0;
        int expired = 0;
        for (MifTraceVerifyLog l : logs) {
            if (l.getVerifyResult() == null) {
                continue;
            }
            if (l.getVerifyResult() == 1) {
                pass++;
            } else if (l.getVerifyResult() == 2) {
                missing++;
            } else if (l.getVerifyResult() == 3) {
                reused++;
            } else if (l.getVerifyResult() == 4) {
                mismatch++;
            } else if (l.getVerifyResult() == 5) {
                expired++;
            }
        }
        vo.setPassCount(pass);
        vo.setMissingCount(missing);
        vo.setReusedCount(reused);
        vo.setMismatchCount(mismatch);
        vo.setExpiredCount(expired);
        vo.setAbnormalCount(logs.size() - pass);
        return vo;
    }

    // ------------------------------------------------------------------

    /** 判定核验结果 */
    private int judge(MifTraceCode c, String itemName, LocalDate bizDate) {
        if (c.getStatus() != null && c.getStatus() == 3) {
            return 6; // 已作废
        }
        if (c.getStatus() != null && c.getStatus() == 2) {
            return 3; // 重复使用
        }
        if (c.getExpireDate() != null && c.getExpireDate().isBefore(bizDate)) {
            return 5; // 已过期
        }
        if (itemName != null && !itemName.isBlank() && c.getDrugName() != null
                && !c.getDrugName().equals(itemName)) {
            return 4; // 与结算项目不符
        }
        return 1;
    }

    private String descOf(int result, MifTraceCode c, String itemName) {
        return switch (result) {
            case 1 -> "追溯码 " + c.getCode() + " 核验通过，药品「" + c.getDrugName() + "」信息一致";
            case 3 -> "追溯码 " + c.getCode() + " 已被结算单 " + nz(c.getBillNo()) + " 核销，涉嫌重复使用";
            case 4 -> "追溯码 " + c.getCode() + " 对应药品「" + nz(c.getDrugName()) + "」与结算项目「"
                    + nz(itemName) + "」不一致，疑似串换药品";
            case 5 -> "追溯码 " + c.getCode() + " 有效期至 " + c.getExpireDate() + "，已过期仍结算";
            case 6 -> "追溯码 " + c.getCode() + " 已作废，不得用于结算";
            default -> "追溯码 " + c.getCode() + " 核验异常";
        };
    }

    private void writeLog(String code, String billNo, String itemName, String drugName, int result, String desc) {
        MifTraceVerifyLog l = new MifTraceVerifyLog();
        l.setCode(code);
        l.setBillNo(billNo);
        l.setItemName(itemName);
        l.setDrugName(drugName);
        l.setVerifyResult(result);
        l.setProblemDesc(desc);
        l.setVerifier(UserContext.username());
        l.setVerifyTime(LocalDateTime.now());
        l.setCreateTime(LocalDateTime.now());
        logMapper.insert(l);
    }

    private TraceCodeVO toCodeVO(MifTraceCode c) {
        TraceCodeVO vo = new TraceCodeVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }

    private TraceLogVO toLogVO(MifTraceVerifyLog l) {
        TraceLogVO vo = new TraceLogVO();
        BeanUtils.copyProperties(l, vo);
        return vo;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }
}
