package com.mis.engine;

import com.mis.entity.MisStockItem;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 库存成本引擎：移动加权平均成本法。
 * <ul>
 *   <li>入库：新均价 =（原库存金额 + 本次入库金额）/（原库存数量 + 本次入库数量）；</li>
 *   <li>出库：出库成本 = 出库数量 × 当前均价，均价不变；</li>
 *   <li>出库数量超过账面库存时允许形成负库存（账实不符信号），但出库成本仍按当前均价结转。</li>
 * </ul>
 */
@Component
public class StockEngine {

    /** 库存变动结果 */
    @Data
    public static class Move {
        private BigDecimal beforeQty;
        private BigDecimal afterQty;
        private BigDecimal beforeAmount;
        private BigDecimal afterAmount;
        /** 变动后移动加权平均单价 */
        private BigDecimal avgPrice;
        /** 本次出入库金额（带符号） */
        private BigDecimal amount;
        /** 异常提示（库存不足等） */
        private String warnMsg;
    }

    /**
     * 计算一次库存变动。
     *
     * @param item   当前库存主档
     * @param qty    变动数量（入库为正，出库为负）
     * @param price  入库单价（出库时忽略，按均价结转）
     * @param amountOverride 出库时直接指定的出库金额（可空，null 表示按均价结转）
     */
    public Move move(MisStockItem item, BigDecimal qty, BigDecimal price, BigDecimal amountOverride) {
        Move m = new Move();
        BigDecimal beforeQty = nz(item.getStockQty());
        BigDecimal beforeAmount = nz(item.getStockAmount());
        BigDecimal beforeAvg = nz(item.getAvgPrice());
        m.setBeforeQty(beforeQty);
        m.setBeforeAmount(beforeAmount);

        boolean inbound = qty.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal absQty = qty.abs();
        BigDecimal amount;

        if (inbound) {
            BigDecimal inAmount = price == null ? BigDecimal.ZERO : price.multiply(absQty);
            BigDecimal afterQty = beforeQty.add(absQty);
            BigDecimal afterAmount = beforeAmount.add(inAmount);
            amount = inAmount;
            m.setAfterQty(afterQty);
            m.setAfterAmount(afterAmount);
            m.setAvgPrice(afterQty.compareTo(BigDecimal.ZERO) > 0
                    ? afterAmount.divide(afterQty, 4, RoundingMode.HALF_UP)
                    : beforeAvg);
        } else {
            BigDecimal outAmount;
            if (amountOverride != null) {
                outAmount = amountOverride;
            } else {
                outAmount = beforeAvg.multiply(absQty);
            }
            BigDecimal afterQty = beforeQty.subtract(absQty);
            BigDecimal afterAmount = beforeAmount.subtract(outAmount);
            amount = outAmount.negate();
            m.setAfterQty(afterQty);
            m.setAfterAmount(afterAmount);
            m.setAvgPrice(beforeAvg);
            if (afterQty.compareTo(BigDecimal.ZERO) < 0) {
                m.setWarnMsg(String.format("账面库存不足（变动前 %.2f，本次出库 %.2f），已形成负库存 %.2f，"
                        + "请及时盘点核查", beforeQty, absQty, afterQty.abs()));
            }
        }
        m.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        return m;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v;
    }
}
