package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.AccountOpDTO;
import com.mis.dto.AccountTxnQueryDTO;
import com.mis.entity.MisInsuredPerson;
import com.mis.entity.MisAccountTxn;
import com.mis.mapper.MisInsuredPersonMapper;
import com.mis.mapper.MisAccountTxnMapper;
import com.mis.service.AccountService;
import com.mis.vo.AccountInfoVO;
import com.mis.vo.AccountTxnVO;
import com.mis.vo.ActionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.mis.entity.MisInsurePayRecord;
import com.mis.mapper.MisInsurePayRecordMapper;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人账户管理：划入 / 结算划扣 / 一次性支取（清退）/ 转移 / 退保冲回
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final MisAccountTxnMapper txnMapper;
    private final MisInsuredPersonMapper personMapper;
    private final MisInsurePayRecordMapper payRecordMapper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    /** 职工医保个人账户划入比例（缴费基数的 8%） */
    private static final BigDecimal ACCOUNT_RATE = new BigDecimal("0.08");

    @Override
    public PageVO<AccountTxnVO> page(AccountTxnQueryDTO q) {
        Page<MisAccountTxn> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisAccountTxn> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MisAccountTxn::getTxnNo, k).or().like(MisAccountTxn::getPersonName, k)
                    .or().like(MisAccountTxn::getIdCard, k).or().like(MisAccountTxn::getReason, k));
        }
        if (StringUtils.hasText(q.getIdCard())) {
            w.eq(MisAccountTxn::getIdCard, q.getIdCard());
        }
        if (q.getTxnType() != null) {
            w.eq(MisAccountTxn::getTxnType, q.getTxnType());
        }
        w.orderByDesc(MisAccountTxn::getId);
        Page<MisAccountTxn> res = txnMapper.selectPage(page, w);
        List<AccountTxnVO> vos = res.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public AccountInfoVO info(String idCard) {
        MisInsuredPerson person = personMapper.selectOne(new LambdaQueryWrapper<MisInsuredPerson>()
                .eq(MisInsuredPerson::getIdCard, idCard)
                .last("limit 1"));
        AccountInfoVO vo = new AccountInfoVO();
        vo.setIdCard(idCard);
        if (person == null) {
            vo.setPersonName(null);
            vo.setBalance(BigDecimal.ZERO);
            vo.setTotalIn(BigDecimal.ZERO);
            vo.setTotalOut(BigDecimal.ZERO);
            vo.setTxnCount(0);
            vo.setTxns(List.of());
            return vo;
        }
        vo.setPersonName(person.getName());
        vo.setBalance(person.getAccountBalance() == null ? BigDecimal.ZERO : person.getAccountBalance());
        List<MisAccountTxn> txns = txnMapper.selectList(new LambdaQueryWrapper<MisAccountTxn>()
                .eq(MisAccountTxn::getIdCard, idCard)
                .orderByDesc(MisAccountTxn::getId));
        BigDecimal in = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
        for (MisAccountTxn t : txns) {
            BigDecimal amt = t.getAmount() == null ? BigDecimal.ZERO : t.getAmount();
            if (amt.compareTo(BigDecimal.ZERO) > 0) {
                in = in.add(amt);
            } else {
                out = out.add(amt.abs());
            }
        }
        vo.setTotalIn(in);
        vo.setTotalOut(out);
        vo.setTxnCount(txns.size());
        vo.setTxns(txns.stream().map(this::toVO).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 一次性支取（清退）/ 转移出：校验参保状态与余额，落流水并同步账户余额。
     */
    @Override
    public ActionVO operate(AccountOpDTO dto) {
        MisInsuredPerson person = personMapper.selectOne(new LambdaQueryWrapper<MisInsuredPerson>()
                .eq(MisInsuredPerson::getIdCard, dto.getIdCard())
                .last("limit 1"));
        if (person == null) {
            return ActionVO.fail("参保人不存在");
        }
        BigDecimal balance = person.getAccountBalance() == null ? BigDecimal.ZERO : person.getAccountBalance();
        BigDecimal amount = dto.getAmount() == null ? BigDecimal.ZERO : dto.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ActionVO.fail("金额必须大于 0");
        }
        if (amount.compareTo(balance) > 0) {
            return ActionVO.fail("支取/转出金额超过个人账户余额 " + balance.toPlainString() + " 元");
        }
        if (dto.getOpType() != null && dto.getOpType() == 3) {
            // 一次性支取（清退）需参保人已停保或终止
            if (person.getPersonStatus() != null && person.getPersonStatus() == 1) {
                return ActionVO.fail("参保人仍处于正常参保状态，不符合一次性支取条件，请先办理停保/终止参保");
            }
            if (dto.getReason() == null || dto.getReason().isBlank()) {
                return ActionVO.fail("一次性支取必须注明事由（死亡/退休/出国定居/终止参保等）");
            }
        }
        BigDecimal after = balance.subtract(amount);
        person.setAccountBalance(after);
        personMapper.updateById(person);

        MisAccountTxn txn = new MisAccountTxn();
        txn.setTxnNo("ZH" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SEQ.incrementAndGet()));
        txn.setIdCard(dto.getIdCard());
        txn.setPersonName(person.getName());
        txn.setTxnType(dto.getOpType());
        txn.setAmount(amount.negate());
        txn.setBalanceAfter(after);
        txn.setTxnTime(LocalDateTime.now());
        txn.setReason(dto.getReason() != null && !dto.getReason().isBlank()
                ? dto.getReason() : (dto.getOpType() == 4 ? "医保关系转移接续个人账户转移" : "个人账户一次性支取"));
        txn.setRefNo(dto.getRefNo());
        txn.setStatus(1);
        txn.setCreateTime(LocalDateTime.now());
        txnMapper.insert(txn);
        return ActionVO.ok();
    }

    private AccountTxnVO toVO(MisAccountTxn t) {
        AccountTxnVO vo = new AccountTxnVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }

    // ------------------------------------------------------------------
    // 缴费划入（个人账户资金的唯一来源）
    // ------------------------------------------------------------------

    /**
     * 从缴费记录划入个人账户。
     * <p>职工医保：按缴费基数 × 划入比例（account_ratio）计算划入额；
     * 缴费记录已带 accountAmount 时以记录为准，否则按比例算。
     * <p>居民医保：无个人账户，不划入。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionVO accrueFromPayRecord(String idCard, List<Long> payRecordIds) {
        MisInsuredPerson person = personMapper.selectOne(new LambdaQueryWrapper<MisInsuredPerson>()
                .eq(MisInsuredPerson::getIdCard, idCard)
                .last("limit 1"));
        if (person == null) {
            return ActionVO.fail("参保人不存在");
        }
        LambdaQueryWrapper<MisInsurePayRecord> w = new LambdaQueryWrapper<>();
        w.eq(MisInsurePayRecord::getIdCard, idCard)
                .eq(MisInsurePayRecord::getPayStatus, 1);
        if (payRecordIds != null && !payRecordIds.isEmpty()) {
            w.in(MisInsurePayRecord::getId, payRecordIds);
        }
        List<MisInsurePayRecord> records = payRecordMapper.selectList(w);
        if (records.isEmpty()) {
            return ActionVO.fail("没有已缴费且可划入的缴费记录");
        }

        BigDecimal balance = person.getAccountBalance() == null ? BigDecimal.ZERO : person.getAccountBalance();
        int done = 0;
        int skipped = 0;
        for (MisInsurePayRecord r : records) {
            // 幂等：同一缴费记录已生成过划入流水则跳过
            String src = sourceNoOf(r);
            Long exists = txnMapper.selectCount(new LambdaQueryWrapper<MisAccountTxn>()
                    .eq(MisAccountTxn::getRefNo, src)
                    .eq(MisAccountTxn::getTxnType, 1));
            if (exists != null && exists > 0) {
                skipped++;
                continue;
            }
            BigDecimal amt = accrueAmountOf(r);
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                skipped++;
                continue;
            }
            balance = balance.add(amt);
            insertTxn(r.getIdCard(), r.getPersonName(), 1, amt, balance,
                    r.getPayMonth() + " 参保缴费划入个人账户", src);
            done++;
        }
        if (done > 0) {
            person.setAccountBalance(balance);
            personMapper.updateById(person);
        }
        if (done == 0 && skipped > 0) {
            return ActionVO.fail("所选缴费记录均已划入过个人账户，未重复入账");
        }
        return ActionVO.matched(done);
    }

    /**
     * 批量划入：把缴费记录中带账户划入金额的记录统一生成流水。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionVO accrueBatch(String payMonth) {
        LambdaQueryWrapper<MisInsurePayRecord> w = new LambdaQueryWrapper<>();
        w.eq(MisInsurePayRecord::getPayStatus, 1);
        if (StringUtils.hasText(payMonth)) {
            w.eq(MisInsurePayRecord::getPayMonth, payMonth);
        }
        List<MisInsurePayRecord> records = payRecordMapper.selectList(w);
        if (records.isEmpty()) {
            return ActionVO.fail("没有符合条件的缴费记录");
        }
        // 按身份证分组，逐人处理（余额需串行更新）
        java.util.Map<String, List<MisInsurePayRecord>> byIdCard = records.stream()
                .filter(r -> StringUtils.hasText(r.getIdCard()))
                .collect(java.util.stream.Collectors.groupingBy(MisInsurePayRecord::getIdCard));

        int total = 0;
        int personCount = 0;
        for (java.util.Map.Entry<String, List<MisInsurePayRecord>> e : byIdCard.entrySet()) {
            MisInsuredPerson person = personMapper.selectOne(new LambdaQueryWrapper<MisInsuredPerson>()
                    .eq(MisInsuredPerson::getIdCard, e.getKey())
                    .last("limit 1"));
            if (person == null) {
                continue;
            }
            BigDecimal balance = person.getAccountBalance() == null ? BigDecimal.ZERO : person.getAccountBalance();
            boolean changed = false;
            for (MisInsurePayRecord r : e.getValue()) {
                String src = sourceNoOf(r);
                Long exists = txnMapper.selectCount(new LambdaQueryWrapper<MisAccountTxn>()
                        .eq(MisAccountTxn::getRefNo, src)
                        .eq(MisAccountTxn::getTxnType, 1));
                if (exists != null && exists > 0) {
                    continue;
                }
                BigDecimal amt = accrueAmountOf(r);
                if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                balance = balance.add(amt);
                insertTxn(r.getIdCard(), r.getPersonName(), 1, amt, balance,
                        r.getPayMonth() + " 参保缴费划入个人账户", src);
                total++;
                changed = true;
            }
            if (changed) {
                person.setAccountBalance(balance);
                personMapper.updateById(person);
                personCount++;
            }
        }
        return ActionVO.matched(total);
    }

    /** 缴费记录唯一标识（用于划入流水幂等） */
    private String sourceNoOf(MisInsurePayRecord r) {
        if (StringUtils.hasText(r.getSourceNo())) {
            return r.getSourceNo();
        }
        return "PAY-" + r.getIdCard() + "-" + r.getPayMonth();
    }

    /**
     * 划入金额：缴费记录已带 accountAmount 时以记录为准；
     * 否则职工医保（缴费档次 ZG 开头）按缴费基数 × 8% 估算，居民医保（JM 开头）不设个人账户。
     */
    private BigDecimal accrueAmountOf(MisInsurePayRecord r) {
        if (r.getAccountAmount() != null && r.getAccountAmount().compareTo(BigDecimal.ZERO) > 0) {
            return r.getAccountAmount();
        }
        String level = r.getPayLevelCode();
        if (level == null || !level.startsWith("ZG")) {
            // 居民医保无个人账户
            return BigDecimal.ZERO;
        }
        if (r.getPayBase() == null || r.getPayBase().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return r.getPayBase().multiply(ACCOUNT_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** 写入一笔账户流水 */
    private void insertTxn(String idCard, String personName, int txnType, BigDecimal amount,
                           BigDecimal balanceAfter, String reason, String refNo) {
        MisAccountTxn txn = new MisAccountTxn();
        txn.setTxnNo("ZH" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SEQ.incrementAndGet()));
        txn.setIdCard(idCard);
        txn.setPersonName(personName);
        txn.setTxnType(txnType);
        txn.setAmount(amount);
        txn.setBalanceAfter(balanceAfter);
        txn.setTxnTime(LocalDateTime.now());
        txn.setReason(reason);
        txn.setRefNo(refNo);
        txn.setStatus(1);
        txn.setCreateTime(LocalDateTime.now());
        txnMapper.insert(txn);
    }
}
