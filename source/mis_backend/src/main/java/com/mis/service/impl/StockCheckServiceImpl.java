package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.StockCheckItemDTO;
import com.mis.dto.StockCheckQueryDTO;
import com.mis.entity.MifStockCheck;
import com.mis.entity.MifStockCheckItem;
import com.mis.entity.MisStockItem;
import com.mis.entity.MisStockTxn;
import com.mis.mapper.MifStockCheckItemMapper;
import com.mis.mapper.MifStockCheckMapper;
import com.mis.mapper.MisStockItemMapper;
import com.mis.mapper.MisStockTxnMapper;
import com.mis.service.StockCheckService;
import com.mis.vo.ActionVO;
import com.mis.vo.StockCheckItemVO;
import com.mis.vo.StockCheckVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import com.mis.config.UserContext;

/**
 * 库存盘点服务实现（与进销存联动）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCheckServiceImpl implements StockCheckService {

    private final MifStockCheckMapper checkMapper;
    private final MifStockCheckItemMapper itemMapper;
    private final MisStockItemMapper stockItemMapper;
    private final MisStockTxnMapper txnMapper;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Override
    public PageVO<StockCheckVO> page(StockCheckQueryDTO q) {
        LambdaQueryWrapper<MifStockCheck> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getCheckNo())) {
            w.like(MifStockCheck::getCheckNo, q.getCheckNo().trim());
        }
        if (StringUtils.hasText(q.getMedinsCode())) {
            w.eq(MifStockCheck::getMedinsCode, q.getMedinsCode());
        }
        if (q.getStatus() != null) {
            w.eq(MifStockCheck::getStatus, q.getStatus());
        }
        if (StringUtils.hasText(q.getCheckDate())) {
            w.eq(MifStockCheck::getCheckDate, q.getCheckDate());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            w.and(x -> x.like(MifStockCheck::getCheckNo, q.getKeyword())
                    .or().like(MifStockCheck::getMedinsName, q.getKeyword()));
        }
        w.orderByDesc(MifStockCheck::getId);

        IPage<MifStockCheck> pg = checkMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        List<StockCheckVO> list = pg.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(list, pg.getTotal());
    }

    /**
     * 生成盘点单：按当前库存快照带出账面数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionVO create(String medinsCode, String medinsName, String checkDate) {
        List<MisStockItem> items = stockItemMapper.selectList(new LambdaQueryWrapper<MisStockItem>()
                .eq(MisStockItem::getStatus, 1));
        if (items.isEmpty()) {
            return ActionVO.fail("库存主档为空，无法生成盘点单");
        }
        String checkNo = "PD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", SEQ.incrementAndGet());
        LocalDate date;
        try {
            date = StringUtils.hasText(checkDate) ? LocalDate.parse(checkDate) : LocalDate.now();
        } catch (Exception e) {
            date = LocalDate.now();
        }

        MifStockCheck c = new MifStockCheck();
        c.setCheckNo(checkNo);
        c.setMedinsCode(medinsCode);
        c.setMedinsName(medinsName);
        c.setCheckDate(date);
        c.setItemCount(items.size());
        c.setDiffCount(0);
        c.setDiffAmount(BigDecimal.ZERO);
        c.setStatus(1);
        c.setChecker(UserContext.username());
        checkMapper.insert(c);

        for (MisStockItem it : items) {
            MifStockCheckItem ci = new MifStockCheckItem();
            ci.setCheckNo(checkNo);
            ci.setItemCode(it.getItemCode());
            ci.setItemName(it.getItemName());
            ci.setBookQty(it.getStockQty() == null ? BigDecimal.ZERO : it.getStockQty());
            ci.setRealQty(null);
            ci.setDiffQty(BigDecimal.ZERO);
            ci.setDiffAmount(BigDecimal.ZERO);
            itemMapper.insert(ci);
        }
        return ActionVO.matched(items.size());
    }

    @Override
    public List<StockCheckItemVO> items(String checkNo) {
        List<MifStockCheckItem> rows = itemMapper.selectList(new LambdaQueryWrapper<MifStockCheckItem>()
                .eq(MifStockCheckItem::getCheckNo, checkNo)
                .orderByAsc(MifStockCheckItem::getId));
        List<StockCheckItemVO> out = new ArrayList<>();
        for (MifStockCheckItem e : rows) {
            StockCheckItemVO vo = new StockCheckItemVO();
            BeanUtils.copyProperties(e, vo);
            out.add(vo);
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionVO submitItem(StockCheckItemDTO dto) {
        return submitItems(List.of(dto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionVO submitItems(List<StockCheckItemDTO> list) {
        if (list == null || list.isEmpty()) {
            return ActionVO.fail("未提交盘点明细");
        }
        MifStockCheck check = findCheck(list.get(0).getCheckNo());
        if (check == null) {
            return ActionVO.fail("盘点单不存在");
        }
        if (check.getStatus() != null && check.getStatus() >= 3) {
            return ActionVO.fail("该盘点单已完成，不能再录入实盘数");
        }
        int n = 0;
        for (StockCheckItemDTO dto : list) {
            MifStockCheckItem ci = itemMapper.selectOne(new LambdaQueryWrapper<MifStockCheckItem>()
                    .eq(MifStockCheckItem::getCheckNo, dto.getCheckNo())
                    .eq(MifStockCheckItem::getItemCode, dto.getItemCode())
                    .last("limit 1"));
            if (ci == null) {
                continue;
            }
            BigDecimal real = dto.getRealQty() == null ? BigDecimal.ZERO : dto.getRealQty();
            ci.setRealQty(real);
            ci.setDiffQty(real.subtract(ci.getBookQty() == null ? BigDecimal.ZERO : ci.getBookQty()));
            ci.setDiffReason(dto.getDiffReason());
            itemMapper.updateById(ci);
            n++;
        }
        if (check.getStatus() != null && check.getStatus() == 1) {
            check.setStatus(2);
            checkMapper.updateById(check);
        }
        return ActionVO.matched(n);
    }

    /**
     * 完成盘点：计算差异金额并调整库存（生成盘点调整流水），状态转待复核。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionVO finish(String checkNo) {
        MifStockCheck check = findCheck(checkNo);
        if (check == null) {
            return ActionVO.fail("盘点单不存在");
        }
        if (check.getStatus() != null && check.getStatus() < 2) {
            return ActionVO.fail("请先录入实盘数量");
        }
        if (check.getStatus() != null && check.getStatus() >= 3) {
            return ActionVO.fail("该盘点单已完成");
        }
        List<MifStockCheckItem> rows = itemMapper.selectList(new LambdaQueryWrapper<MifStockCheckItem>()
                .eq(MifStockCheckItem::getCheckNo, checkNo));
        if (rows.isEmpty()) {
            return ActionVO.fail("盘点单无明细");
        }

        int diffCount = 0;
        BigDecimal diffAmount = BigDecimal.ZERO;
        for (MifStockCheckItem ci : rows) {
            if (ci.getRealQty() == null) {
                continue;
            }
            BigDecimal book = ci.getBookQty() == null ? BigDecimal.ZERO : ci.getBookQty();
            BigDecimal diff = ci.getRealQty().subtract(book);
            ci.setDiffQty(diff);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                diffCount++;
                MisStockItem stock = stockItemMapper.selectOne(new LambdaQueryWrapper<MisStockItem>()
                        .eq(MisStockItem::getItemCode, ci.getItemCode()).last("limit 1"));
                BigDecimal price = stock == null || stock.getAvgPrice() == null
                        ? BigDecimal.ZERO : stock.getAvgPrice();
                BigDecimal amt = diff.multiply(price).setScale(2, java.math.RoundingMode.HALF_UP);
                ci.setDiffAmount(amt);
                diffAmount = diffAmount.add(amt);
                // 调整库存：正差异补入，负差异冲减，生成盘点调整流水
                if (stock != null) {
                    adjustStock(stock, diff, price, checkNo);
                }
            } else {
                ci.setDiffAmount(BigDecimal.ZERO);
            }
            itemMapper.updateById(ci);
        }
        check.setDiffCount(diffCount);
        check.setDiffAmount(diffAmount);
        check.setStatus(3);
        checkMapper.updateById(check);

        // 待复核 → 待办
        TodoServiceImpl.pushTodo(
                "mif_stock_check",
                "库存盘点待复核：" + checkNo + " 差异品项 " + diffCount + " 项，差异金额 " + diffAmount + " 元",
                "mif_stock_check", check.getId(), checkNo,
                "审核员B", 3L, diffCount > 0 ? 1 : 3);
        return ActionVO.matched(diffCount);
    }

    /**
     * 复核：确认则办结并关闭待办；驳回则退回待盘点（库存已调整的需人工反向处理，故此处仅改状态并留痕）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionVO review(String checkNo, boolean pass, String opinion) {
        MifStockCheck check = findCheck(checkNo);
        if (check == null) {
            return ActionVO.fail("盘点单不存在");
        }
        if (check.getStatus() == null || check.getStatus() != 3) {
            return ActionVO.fail("仅待复核的盘点单可复核");
        }
        check.setStatus(pass ? 4 : 2);
        check.setReviewer(UserContext.username());
        check.setReviewTime(LocalDateTime.now());
        checkMapper.updateById(check);
        if (pass) {
            TodoServiceImpl.closeTodo("mif_stock_check", check.getId(), "盘点差异已复核确认");
        }
        return ActionVO.ok();
    }

    /** 按差异调整库存并写盘点调整流水 */
    private void adjustStock(MisStockItem stock, BigDecimal diffQty, BigDecimal price, String checkNo) {
        BigDecimal beforeQty = stock.getStockQty() == null ? BigDecimal.ZERO : stock.getStockQty();
        BigDecimal beforeAmount = stock.getStockAmount() == null ? BigDecimal.ZERO : stock.getStockAmount();
        BigDecimal afterQty = beforeQty.add(diffQty);
        BigDecimal afterAmount = beforeAmount.add(diffQty.multiply(price));

        stock.setStockQty(afterQty);
        stock.setStockAmount(afterAmount);
        if (afterQty.compareTo(BigDecimal.ZERO) != 0) {
            stock.setAvgPrice(afterAmount.divide(afterQty, 4, java.math.RoundingMode.HALF_UP));
        }
        stockItemMapper.updateById(stock);

        MisStockTxn txn = new MisStockTxn();
        txn.setTxnNo("PD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SEQ.incrementAndGet()));
        txn.setItemId(stock.getId());
        txn.setItemCode(stock.getItemCode());
        txn.setItemName(stock.getItemName());
        txn.setTxnType(5);
        txn.setQty(diffQty);
        txn.setPrice(price);
        txn.setAmount(diffQty.multiply(price).setScale(2, java.math.RoundingMode.HALF_UP));
        txn.setBeforeQty(beforeQty);
        txn.setAfterQty(afterQty);
        txn.setRefNo(checkNo);
        txn.setBizDate(LocalDate.now());
        txn.setOperator(UserContext.username());
        txn.setRemark("库存盘点差异调整" + (diffQty.compareTo(BigDecimal.ZERO) > 0 ? "（盘盈）" : "（盘亏）"));
        txn.setCreateTime(LocalDateTime.now());
        txnMapper.insert(txn);
    }

    private MifStockCheck findCheck(String checkNo) {
        return checkMapper.selectOne(new LambdaQueryWrapper<MifStockCheck>()
                .eq(MifStockCheck::getCheckNo, checkNo).last("limit 1"));
    }

    private StockCheckVO toVO(MifStockCheck e) {
        StockCheckVO vo = new StockCheckVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
