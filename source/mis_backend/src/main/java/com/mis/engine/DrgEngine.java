package com.mis.engine;

import com.mis.entity.MisBill;
import com.mis.entity.MifDrgGroup;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * DRG/DIP 支付监管引擎：病例入组匹配、支付标准测算与偏差（超支）判定。
 * <p>入组口径：按实际费用与病组/病种的组均次费用最接近者匹配（同费用档优先）。
 * DRG 支付标准 = 权重 × 费率；DIP 支付标准 = 分值 × 每分值成本。
 */
@Component
public class DrgEngine {

    /** 偏差预警阈值（偏差率超过该值预警） */
    public static final BigDecimal WARN_DEVIATION = new BigDecimal("0.2");
    /** 费用超支阈值（偏差率超过该值判定超支） */
    public static final BigDecimal OVER_DEVIATION = new BigDecimal("0.5");

    /** 入组结果 */
    @Data
    public static class Graded {
        private MifDrgGroup group;
        /** 权重 / 分值 */
        private BigDecimal weight;
        /** 预估支付标准 */
        private BigDecimal estCost;
        /** 实际费用 */
        private BigDecimal actualCost;
        /** 偏差率 */
        private BigDecimal deviationRate;
        /** 状态（1-正常 2-偏差预警 3-费用超支） */
        private Integer status;
        private String reason;
    }

    /**
     * 对一笔住院结算单入组。
     *
     * @param bill   住院结算单
     * @param groups 已启用的病组/病种池
     */
    public Graded grade(MisBill bill, List<MifDrgGroup> groups) {
        Graded g = new Graded();
        BigDecimal actual = nz(bill.getTotalAmount());
        g.setActualCost(actual);
        if (groups == null || groups.isEmpty()) {
            g.setStatus(1);
            g.setReason("未配置病组/病种池，暂无法入组");
            return g;
        }
        // 优先匹配与本次支付方式一致的病组
        MifDrgGroup best = null;
        BigDecimal bestDiff = null;
        for (MifDrgGroup gr : groups) {
            BigDecimal avg = nz(gr.getAvgCost());
            if (avg.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal diff = avg.subtract(actual).abs();
            if (bestDiff == null || diff.compareTo(bestDiff) < 0) {
                bestDiff = diff;
                best = gr;
            }
        }
        if (best == null) {
            g.setStatus(1);
            g.setReason("病组/病种池缺少组均次费用，无法匹配入组");
            return g;
        }
        g.setGroup(best);
        BigDecimal est;
        BigDecimal weight;
        if (best.getPayType() != null && best.getPayType() == 2) {
            weight = nz(best.getPointValue());
            est = weight.multiply(nz(best.getPointCost()));
        } else {
            weight = nz(best.getWeight());
            est = weight.multiply(nz(best.getRate()));
        }
        g.setWeight(weight);
        g.setEstCost(est.setScale(2, RoundingMode.HALF_UP));
        if (est.compareTo(BigDecimal.ZERO) <= 0) {
            g.setStatus(1);
            g.setReason("病组/病种未配置权重费率或分值成本，暂无法测算支付标准");
            return g;
        }
        BigDecimal dev = actual.subtract(est)
                .divide(est, 4, RoundingMode.HALF_UP);
        g.setDeviationRate(dev);
        double devPct = dev.doubleValue() * 100;
        if (dev.compareTo(OVER_DEVIATION) > 0) {
            g.setStatus(3);
            g.setReason(String.format("实际费用 %s 元超出支付标准 %s 元（偏差 %.1f%%，超过 %.0f%%），存在费用超支风险",
                    plain(actual), plain(est), devPct, OVER_DEVIATION.multiply(new BigDecimal("100")).doubleValue()));
        } else if (dev.compareTo(WARN_DEVIATION) > 0) {
            g.setStatus(2);
            g.setReason(String.format("实际费用 %s 元与支付标准 %s 元偏差 %.1f%%，超过 %.0f%% 预警线",
                    plain(actual), plain(est), devPct, WARN_DEVIATION.multiply(new BigDecimal("100")).doubleValue()));
        } else {
            g.setStatus(1);
            g.setReason(String.format("实际费用 %s 元与支付标准 %s 元偏差 %.1f%%，在合理区间",
                    plain(actual), plain(est), devPct));
        }
        return g;
    }

    /** 供前端展示的偏差率百分比 */
    public static BigDecimal percent(BigDecimal deviationRate) {
        if (deviationRate == null) {
            return BigDecimal.ZERO;
        }
        return deviationRate.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v;
    }

    private String plain(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
