package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.AccountOpDTO;
import com.mis.dto.AccountTxnQueryDTO;
import com.mis.vo.AccountInfoVO;
import com.mis.vo.AccountTxnVO;
import com.mis.vo.ActionVO;

import java.util.List;

public interface AccountService {

    PageVO<AccountTxnVO> page(AccountTxnQueryDTO q);

    /** 个人账户查询（余额 + 累计划入/支出 + 明细） */
    AccountInfoVO info(String idCard);

    /** 一次性支取（清退）或转移出 */
    ActionVO operate(AccountOpDTO dto);

    /**
     * 从参保缴费记录划入个人账户。
     * <p>缴费记录（mis_insure_pay_record）是个人账户资金的唯一来源：
     * 职工医保按缴费基数的固定比例划入，居民医保不划入。
     * <p>已划入过的缴费记录不重复入账（按缴费记录 source_no 幂等）。
     *
     * @param idCard         身份证号
     * @param payRecordIds  待划入的缴费记录ID，为空表示划入该人全部未划入记录
     */
    ActionVO accrueFromPayRecord(String idCard, List<Long> payRecordIds);

    /**
     * 批量划入：对指定月份范围的所有参保缴费记录生成个人账户划入流水。
     *
     * @param payMonth 缴费月份 yyyy-MM，为空表示全部未划入记录
     */
    ActionVO accrueBatch(String payMonth);
}
