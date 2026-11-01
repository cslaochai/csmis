package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.config.UserContext;
import com.mis.dto.StockItemQueryDTO;
import com.mis.dto.StockItemUpsertDTO;
import com.mis.dto.StockOpDTO;
import com.mis.dto.StockTxnQueryDTO;
import com.mis.engine.StockEngine;
import com.mis.entity.MisBill;
import com.mis.entity.MisFeeItem;
import com.mis.entity.MisStockItem;
import com.mis.entity.MisStockTxn;
import com.mis.mapper.MisStockItemMapper;
import com.mis.mapper.MisStockTxnMapper;
import com.mis.service.StockService;
import com.mis.vo.ActionVO;
import com.mis.vo.StockItemVO;
import com.mis.vo.StockStatsVO;
import com.mis.vo.StockTrendVO;
import com.mis.vo.StockTxnVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 药品耗材进销存：主档维护、出入库与盘点、结算联动出库/退费回滚、库存预警
 */
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final MisStockItemMapper itemMapper;
    private final MisStockTxnMapper txnMapper;
    private final StockEngine stockEngine;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    /** 需纳入库存管理的项目类别 */
    private static final List<String> STOCK_CATEGORIES = List.of("西药", "中成药", "中草药", "医用耗材");

    // ------------------------------------------------------------------ 主档

    @Override
    public PageVO<StockItemVO> pageItem(StockItemQueryDTO q) {
        Page<MisStockItem> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisStockItem> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MisStockItem::getItemCode, k).or().like(MisStockItem::getItemName, k)
                    .or().like(MisStockItem::getYbCode, k).or().like(MisStockItem::getManufacturer, k));
        }
        if (StringUtils.hasText(q.getCategory())) {
            w.eq(MisStockItem::getCategory, q.getCategory());
        }
        if (q.getStatus() != null) {
            w.eq(MisStockItem::getStatus, q.getStatus());
        }
        if (Boolean.TRUE.equals(q.getOnlyAbnormal())) {
            w.lt(MisStockItem::getStockQty, BigDecimal.ZERO);
        }
        w.orderByDesc(MisStockItem::getId);
        Page<MisStockItem> res = itemMapper.selectPage(page, w);
        List<StockItemVO> vos = res.getRecords().stream().map(this::toItemVO).collect(Collectors.toList());
        // 预警标记与近 30 天出库量
        Map<Long, BigDecimal> outQty30 = outQtyByItem30();
        for (StockItemVO vo : vos) {
            BigDecimal warn = vo.getWarnQty() == null ? BigDecimal.ZERO : vo.getWarnQty();
            BigDecimal qty = vo.getStockQty() == null ? BigDecimal.ZERO : vo.getStockQty();
            vo.setWarnFlag(warn.compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(warn) < 0);
            vo.setAbnormalFlag(qty.compareTo(BigDecimal.ZERO) < 0);
            vo.setOutQty30(outQty30.getOrDefault(vo.getId(), BigDecimal.ZERO));
        }
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO upsertItem(StockItemUpsertDTO dto) {
        Long dup = itemMapper.selectCount(new LambdaQueryWrapper<MisStockItem>()
                .eq(MisStockItem::getItemCode, dto.getItemCode())
                .ne(dto.getId() != null, MisStockItem::getId, dto.getId()));
        if (dup != null && dup > 0) {
            return ActionVO.fail("品项编码已存在");
        }
        MisStockItem it;
        if (dto.getId() == null) {
            it = new MisStockItem();
            it.setStockQty(BigDecimal.ZERO);
            it.setStockAmount(BigDecimal.ZERO);
            it.setAvgPrice(BigDecimal.ZERO);
        } else {
            it = itemMapper.selectById(dto.getId());
            if (it == null) {
                return ActionVO.fail("库存主档不存在");
            }
        }
        it.setItemCode(dto.getItemCode());
        it.setItemName(dto.getItemName());
        it.setCategory(dto.getCategory());
        it.setSpec(dto.getSpec());
        it.setUnit(dto.getUnit());
        it.setYbCode(dto.getYbCode());
        it.setManufacturer(dto.getManufacturer());
        it.setWarnQty(dto.getWarnQty());
        it.setLocation(dto.getLocation());
        it.setStatus(dto.getStatus());
        it.setRemark(dto.getRemark());
        it.setUpdateTime(LocalDateTime.now());
        if (dto.getId() == null) {
            it.setCreateTime(LocalDateTime.now());
            itemMapper.insert(it);
        } else {
            itemMapper.updateById(it);
        }
        return ActionVO.ok();
    }

    @Override
    public ActionVO changeItemStatus(Long id, Integer status) {
        MisStockItem it = itemMapper.selectById(id);
        if (it == null) {
            return ActionVO.fail("库存主档不存在");
        }
        it.setStatus(status);
        it.setUpdateTime(LocalDateTime.now());
        itemMapper.updateById(it);
        return ActionVO.ok();
    }

    // ------------------------------------------------------------------ 流水

    @Override
    public PageVO<StockTxnVO> pageTxn(StockTxnQueryDTO q) {
        Page<MisStockTxn> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<MisStockTxn> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String k = q.getKeyword();
            w.and(x -> x.like(MisStockTxn::getTxnNo, k).or().like(MisStockTxn::getItemCode, k)
                    .or().like(MisStockTxn::getItemName, k).or().like(MisStockTxn::getRefNo, k));
        }
        if (q.getItemId() != null) {
            w.eq(MisStockTxn::getItemId, q.getItemId());
        }
        if (q.getTxnType() != null) {
            w.eq(MisStockTxn::getTxnType, q.getTxnType());
        }
        if (StringUtils.hasText(q.getRefNo())) {
            w.eq(MisStockTxn::getRefNo, q.getRefNo());
        }
        w.orderByDesc(MisStockTxn::getId);
        Page<MisStockTxn> res = txnMapper.selectPage(page, w);
        List<StockTxnVO> vos = res.getRecords().stream().map(this::toTxnVO).collect(Collectors.toList());
        return PageVO.of(vos, res.getTotal());
    }

    @Override
    public ActionVO operate(StockOpDTO dto) {
        MisStockItem item = findItem(dto.getItemCode());
        if (item == null) {
            return ActionVO.fail("库存主档不存在：" + dto.getItemCode());
        }
        if (dto.getOpType() == null) {
            return ActionVO.fail("操作类型不能为空");
        }
        BigDecimal qty = dto.getQty() == null ? BigDecimal.ZERO : dto.getQty();
        BigDecimal price = dto.getPrice();
        BigDecimal signedQty;
        BigDecimal amountOverride = null;

        switch (dto.getOpType()) {
            case 1, 3 -> {
                // 采购入库 / 退药入库
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    return ActionVO.fail("入库数量必须大于 0");
                }
                if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                    return ActionVO.fail("入库必须填写单价");
                }
                signedQty = qty;
            }
            case 2, 4 -> {
                // 销售出库 / 报损出库
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    return ActionVO.fail("出库数量必须大于 0");
                }
                signedQty = qty.negate();
            }
            case 5 -> {
                // 盘点调整：qty 为实盘数量，差额入账
                BigDecimal diff = qty.subtract(nz(item.getStockQty()));
                if (diff.compareTo(BigDecimal.ZERO) == 0) {
                    return ActionVO.fail("实盘数量与账面库存一致，无需调整");
                }
                signedQty = diff;
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    // 盘盈：按当前均价补足库存金额
                    price = item.getAvgPrice();
                    amountOverride = diff.multiply(nz(item.getAvgPrice()));
                }
            }
            default -> {
                return ActionVO.fail("不支持的操作类型");
            }
        }

        StockEngine.Move mv = stockEngine.move(item, signedQty, price, amountOverride);
        // 落库存主档
        item.setStockQty(mv.getAfterQty());
        item.setStockAmount(mv.getAfterAmount());
        item.setAvgPrice(mv.getAvgPrice());
        item.setUpdateTime(LocalDateTime.now());
        itemMapper.updateById(item);

        // 落流水
        MisStockTxn t = new MisStockTxn();
        t.setTxnNo(genTxnNo());
        t.setItemId(item.getId());
        t.setItemCode(item.getItemCode());
        t.setItemName(item.getItemName());
        t.setTxnType(dto.getOpType());
        t.setQty(signedQty);
        t.setPrice(price);
        t.setAmount(mv.getAmount());
        t.setBeforeQty(mv.getBeforeQty());
        t.setAfterQty(mv.getAfterQty());
        t.setBatchNo(dto.getBatchNo());
        t.setExpireDate(dto.getExpireDate());
        t.setRefNo(dto.getRefNo());
        t.setBizDate(dto.getBizDate() != null ? dto.getBizDate() : LocalDate.now());
        t.setOperator(UserContext.username());
        t.setWarnMsg(mv.getWarnMsg());
        t.setRemark(dto.getRemark());
        t.setCreateTime(LocalDateTime.now());
        txnMapper.insert(t);
        return ActionVO.ok();
    }

    // ------------------------------------------------------------------ 结算联动

    /**
     * 结算出库：对结算单中的药品/耗材类明细逐条扣减库存。
     * <p>匹配规则：先按「品项名称 + 规格」匹配库存主档，再退化为按医保编码匹配；都匹配不上则跳过。
     * 库存不足时不阻断结算（临床已实际用药），而是形成负库存并在流水中标记异常，交由盘点核查。
     */
    @Override
    public int issueForBill(MisBill bill, List<MisFeeItem> items) {
        if (bill == null || items == null || items.isEmpty()) {
            return 0;
        }
        Map<String, MisStockItem> byNameSpec = new HashMap<>();
        Map<String, MisStockItem> byCode = new HashMap<>();
        for (MisStockItem it : itemMapper.selectList(null)) {
            byNameSpec.put(nz(it.getItemName()) + "|" + nz(it.getSpec()), it);
            if (!blank(it.getYbCode())) {
                byCode.put(it.getYbCode(), it);
            }
        }
        int issued = 0;
        LocalDate bizDate = bill.getSettleTime() == null ? LocalDate.now() : bill.getSettleTime().toLocalDate();
        for (MisFeeItem it : items) {
            if (it.getCategory() == null || !STOCK_CATEGORIES.contains(it.getCategory())) {
                continue;
            }
            MisStockItem stock = byNameSpec.get(nz(it.getName()) + "|" + nz(it.getSpec()));
            if (stock == null) {
                stock = byCode.get(nz(it.getYbCode()));
            }
            if (stock == null) {
                continue;
            }
            BigDecimal qty = it.getQty() == null ? BigDecimal.ZERO : BigDecimal.valueOf(it.getQty());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            StockEngine.Move mv = stockEngine.move(stock, qty.negate(), null, null);
            stock.setStockQty(mv.getAfterQty());
            stock.setStockAmount(mv.getAfterAmount());
            stock.setAvgPrice(mv.getAvgPrice());
            stock.setUpdateTime(LocalDateTime.now());
            itemMapper.updateById(stock);

            MisStockTxn t = new MisStockTxn();
            t.setTxnNo(genTxnNo());
            t.setItemId(stock.getId());
            t.setItemCode(stock.getItemCode());
            t.setItemName(stock.getItemName());
            t.setTxnType(2);
            t.setQty(qty.negate());
            t.setPrice(mv.getAvgPrice());
            t.setAmount(mv.getAmount());
            t.setBeforeQty(mv.getBeforeQty());
            t.setAfterQty(mv.getAfterQty());
            t.setRefNo(bill.getBillNo());
            t.setBizDate(bizDate);
            t.setOperator(UserContext.username());
            t.setWarnMsg(mv.getWarnMsg());
            t.setRemark("结算出库：" + nz(bill.getPatientName()) + " " + nz(it.getName()));
            t.setCreateTime(LocalDateTime.now());
            txnMapper.insert(t);
            issued++;
        }
        return issued;
    }

    /**
     * 退费回滚：把该结算单产生的出库流水红冲入库，回补库存。
     */
    @Override
    public int rollbackForBill(String billNo) {
        if (blank(billNo)) {
            return 0;
        }
        List<MisStockTxn> outs = txnMapper.selectList(new LambdaQueryWrapper<MisStockTxn>()
                .eq(MisStockTxn::getRefNo, billNo)
                .eq(MisStockTxn::getTxnType, 2));
        int rolled = 0;
        for (MisStockTxn out : outs) {
            MisStockItem stock = out.getItemId() == null ? null : itemMapper.selectById(out.getItemId());
            if (stock == null) {
                continue;
            }
            BigDecimal backQty = nz(out.getQty()).negate();
            StockEngine.Move mv = stockEngine.move(stock, backQty, out.getPrice(), null);
            stock.setStockQty(mv.getAfterQty());
            stock.setStockAmount(mv.getAfterAmount());
            stock.setAvgPrice(mv.getAvgPrice());
            stock.setUpdateTime(LocalDateTime.now());
            itemMapper.updateById(stock);

            MisStockTxn t = new MisStockTxn();
            t.setTxnNo(genTxnNo());
            t.setItemId(stock.getId());
            t.setItemCode(stock.getItemCode());
            t.setItemName(stock.getItemName());
            t.setTxnType(3);
            t.setQty(backQty);
            t.setPrice(out.getPrice());
            t.setAmount(mv.getAmount());
            t.setBeforeQty(mv.getBeforeQty());
            t.setAfterQty(mv.getAfterQty());
            t.setRefNo(billNo);
            t.setBizDate(LocalDate.now());
            t.setOperator(UserContext.username());
            t.setRemark("退费红冲入库：" + nz(out.getItemName()));
            t.setCreateTime(LocalDateTime.now());
            txnMapper.insert(t);
            rolled++;
        }
        return rolled;
    }

    // ------------------------------------------------------------------ 统计

    @Override
    public StockStatsVO stats() {
        List<MisStockItem> items = itemMapper.selectList(null);
        StockStatsVO vo = new StockStatsVO();
        vo.setItemCount(items.size());
        int stocked = 0;
        int warn = 0;
        int abnormal = 0;
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        for (MisStockItem it : items) {
            BigDecimal q = nz(it.getStockQty());
            BigDecimal w = nz(it.getWarnQty());
            qty = qty.add(q);
            amount = amount.add(nz(it.getStockAmount()));
            if (q.compareTo(BigDecimal.ZERO) > 0) {
                stocked++;
            }
            if (w.compareTo(BigDecimal.ZERO) > 0 && q.compareTo(w) < 0) {
                warn++;
            }
            if (q.compareTo(BigDecimal.ZERO) < 0) {
                abnormal++;
            }
        }
        vo.setStockedItemCount(stocked);
        vo.setWarnCount(warn);
        vo.setAbnormalCount(abnormal);
        vo.setTotalQty(qty);
        vo.setTotalAmount(amount);

        LocalDate from = LocalDate.now().minusDays(29);
        List<MisStockTxn> txns = txnMapper.selectList(new LambdaQueryWrapper<MisStockTxn>()
                .ge(MisStockTxn::getBizDate, from));
        BigDecimal inAmount = BigDecimal.ZERO;
        BigDecimal outAmount = BigDecimal.ZERO;
        BigDecimal inQty = BigDecimal.ZERO;
        BigDecimal outQty = BigDecimal.ZERO;
        int inCount = 0;
        int outCount = 0;
        int diffCount = 0;
        int takeCount = 0;
        for (MisStockTxn t : txns) {
            BigDecimal amt = nz(t.getAmount());
            BigDecimal q = nz(t.getQty());
            if (q.compareTo(BigDecimal.ZERO) > 0) {
                inAmount = inAmount.add(amt);
                inQty = inQty.add(q);
                inCount++;
            } else {
                outAmount = outAmount.add(amt.abs());
                outQty = outQty.add(q.abs());
                outCount++;
            }
            if (t.getTxnType() != null && t.getTxnType() == 5) {
                takeCount++;
                if (q.compareTo(BigDecimal.ZERO) != 0) {
                    diffCount++;
                }
            }
        }
        vo.setInAmount30(inAmount);
        vo.setOutAmount30(outAmount);
        vo.setInQty30(inQty);
        vo.setOutQty30(outQty);
        vo.setInCount30(inCount);
        vo.setOutCount30(outCount);
        vo.setStocktakeDiffCount(diffCount);
        vo.setMatchRate(takeCount == 0 ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf(takeCount - diffCount)
                        .multiply(new BigDecimal("100"))
                        .divide(BigDecimal.valueOf(takeCount), 2, RoundingMode.HALF_UP));
        return vo;
    }

    @Override
    public List<StockTrendVO> trend(int rangeDays) {
        int days = rangeDays <= 0 ? 30 : rangeDays;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        Map<String, StockTrendVO> map = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            StockTrendVO p = new StockTrendVO();
            p.setDay(d.toString());
            p.setInAmount(BigDecimal.ZERO);
            p.setOutAmount(BigDecimal.ZERO);
            p.setInQty(BigDecimal.ZERO);
            p.setOutQty(BigDecimal.ZERO);
            map.put(d.toString(), p);
        }
        for (MisStockTxn t : txnMapper.selectList(new LambdaQueryWrapper<MisStockTxn>()
                .ge(MisStockTxn::getBizDate, start))) {
            StockTrendVO p = map.get(String.valueOf(t.getBizDate()));
            if (p == null) {
                continue;
            }
            BigDecimal amt = nz(t.getAmount());
            BigDecimal q = nz(t.getQty());
            if (q.compareTo(BigDecimal.ZERO) > 0) {
                p.setInAmount(p.getInAmount().add(amt));
                p.setInQty(p.getInQty().add(q));
            } else {
                p.setOutAmount(p.getOutAmount().add(amt.abs()));
                p.setOutQty(p.getOutQty().add(q.abs()));
            }
        }
        return new ArrayList<>(map.values());
    }

    // ------------------------------------------------------------------

    /** 查找库存主档：优先按编码，其次按名称+规格 */
    private MisStockItem findItem(String itemCode) {
        MisStockItem it = itemMapper.selectOne(new LambdaQueryWrapper<MisStockItem>()
                .eq(MisStockItem::getItemCode, itemCode)
                .last("limit 1"));
        if (it == null) {
            it = itemMapper.selectOne(new LambdaQueryWrapper<MisStockItem>()
                    .eq(MisStockItem::getItemName, itemCode)
                    .last("limit 1"));
        }
        return it;
    }

    private Map<Long, BigDecimal> outQtyByItem30() {
        Map<Long, BigDecimal> map = new HashMap<>();
        LocalDate from = LocalDate.now().minusDays(29);
        for (MisStockTxn t : txnMapper.selectList(new LambdaQueryWrapper<MisStockTxn>()
                .ge(MisStockTxn::getBizDate, from)
                .lt(MisStockTxn::getQty, BigDecimal.ZERO))) {
            if (t.getItemId() == null) {
                continue;
            }
            map.merge(t.getItemId(), nz(t.getQty()).abs(), BigDecimal::add);
        }
        return map;
    }

    private String genTxnNo() {
        return "KC" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SEQ.incrementAndGet());
    }

    private StockItemVO toItemVO(MisStockItem it) {
        StockItemVO vo = new StockItemVO();
        BeanUtils.copyProperties(it, vo);
        return vo;
    }

    private StockTxnVO toTxnVO(MisStockTxn t) {
        StockTxnVO vo = new StockTxnVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
