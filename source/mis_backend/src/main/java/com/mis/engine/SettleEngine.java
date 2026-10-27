package com.mis.engine;

import com.mis.entity.MssFeeItem;
import com.mis.vo.DecVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 经办窗口医保费用分解（即时结算）。
 * 规则：甲类全额计入、乙类先按自付比例扣除、丙类全额自费；
 * 扣除起付线后按险种比例报销统筹；职工医保再从个账划扣一部分。
 */
@Component
public class SettleEngine {

    private static final BigDecimal ACCOUNT_RATIO = new BigDecimal("0.4");
    private static final BigDecimal ACCOUNT_MAX = new BigDecimal("1500");

    public DecVO compute(List<MssFeeItem> items, String insuranceType) {
        // 参保人表 insurance_type 可能存码值（2-城镇职工 3-城乡居民）也可能是文字（经办窗口录入），先归一
        String type = normalizeInsuranceType(insuranceType);
        BigDecimal total = z();
        BigDecimal basePool = z();
        BigDecimal selfPay = z();
        BigDecimal fullySelf = z();

        if (items != null) {
            for (MssFeeItem it : items) {
                BigDecimal price = it.getPrice() == null ? z() : it.getPrice();
                BigDecimal qty = it.getQty() == null ? BigDecimal.ONE : new BigDecimal(it.getQty());
                BigDecimal amount = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                it.setAmount(amount);
                total = total.add(amount);

                if ("丙".equals(it.getPayClass())) {
                    fullySelf = fullySelf.add(amount);
                } else {
                    BigDecimal ratio = it.getSelfRatio() == null ? z() : it.getSelfRatio();
                    BigDecimal selfPart = amount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                    basePool = basePool.add(amount.subtract(selfPart));
                    selfPay = selfPay.add(selfPart);
                }
            }
        }

        BigDecimal deductible = deductible(type);
        BigDecimal insRatio = ratio(type);
        BigDecimal effectiveBase = basePool.subtract(deductible).max(z());
        BigDecimal insurancePay = effectiveBase.multiply(insRatio).setScale(2, RoundingMode.HALF_UP);
        // 起付线 + 报销后个人自付的统筹段
        selfPay = selfPay.add(basePool.subtract(insurancePay));

        BigDecimal personalCashPay = fullySelf.add(selfPay);
        BigDecimal personalAccount = z();
        BigDecimal cashPay = personalCashPay;
        if ("职工医保".equals(type)) {
            personalAccount = personalCashPay.multiply(ACCOUNT_RATIO).min(ACCOUNT_MAX).setScale(2, RoundingMode.HALF_UP);
            cashPay = personalCashPay.subtract(personalAccount);
        }

        DecVO d = new DecVO();
        d.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        d.setInsurancePay(insurancePay);
        d.setPersonalAccount(personalAccount);
        d.setCashPay(cashPay.setScale(2, RoundingMode.HALF_UP));
        d.setSelfPay(personalCashPay.setScale(2, RoundingMode.HALF_UP));
        d.setDeductible(deductible);
        d.setBasePool(basePool.setScale(2, RoundingMode.HALF_UP));
        return d;
    }

    /** 码值与文字双兼容：2/城镇职工医保→职工医保，3/城乡居民医保→居民医保，其余原样（异地就医等） */
    private String normalizeInsuranceType(String t) {
        if (t == null) {
            return null;
        }
        switch (t) {
            case "2":
            case "城镇职工医保":
                return "职工医保";
            case "3":
            case "城乡居民医保":
                return "居民医保";
            default:
                return t;
        }
    }

    private BigDecimal deductible(String insuranceType) {
        if ("职工医保".equals(insuranceType)) {
            return new BigDecimal("300");
        }
        if ("居民医保".equals(insuranceType)) {
            return new BigDecimal("500");
        }
        return new BigDecimal("800");
    }

    private BigDecimal ratio(String insuranceType) {
        if ("职工医保".equals(insuranceType)) {
            return new BigDecimal("0.85");
        }
        if ("居民医保".equals(insuranceType)) {
            return new BigDecimal("0.70");
        }
        return new BigDecimal("0.60");
    }

    private static BigDecimal z() {
        return BigDecimal.ZERO;
    }
}
