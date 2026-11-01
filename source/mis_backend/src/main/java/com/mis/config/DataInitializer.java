package com.mis.config;

import com.mis.engine.SettleEngine;
import com.mis.entity.*;
import com.mis.mapper.*;
import com.mis.vo.DecVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 经办业务演示数据初始化（幂等：仅在目录为空时生成一次）。
 * 用固定种子的 LCG 保证每次启动数据一致；日期以当前日为基准动态落到近 10 天，
 * 保证经办工作台“今日”KPI 有数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MisCatalogMapper catalogMapper;
    private final MisItemMappingMapper mappingMapper;
    private final MisPendingSettleMapper pendingMapper;
    private final MisBillMapper billMapper;
    private final MisFeeItemMapper feeItemMapper;
    private final MisFeeAuditMapper auditMapper;
    private final MisFilingMapper filingMapper;
    private final MisReconMapper reconMapper;
    private final MisStockItemMapper stockItemMapper;
    private final MisStockTxnMapper stockTxnMapper;
    private final SettleEngine settleEngine;

    private long seed = 20261013L;

    private static final List<String> INSURANCE_TYPES = List.of("职工医保", "居民医保", "异地就医");
    private static final List<String> SETTLE_TYPES = List.of("门诊", "住院");
    private static final List<String> DEPTS =
            List.of("门诊内科", "门诊外科", "门诊儿科", "门诊骨科", "急诊科", "心血管内科", "呼吸内科");
    private static final List<String> DOCTORS =
            List.of("张建国", "李秀英", "王伟", "陈静", "刘强", "赵敏", "孙磊");
    private static final String SURNAMES = "赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜";
    private static final String GIVEN = "伟芳娜敏静秀强磊军洋勇艳杰娟涛明超霞平刚桂英文辉建国";
    private static final List<String> PAY_CLASS = List.of("甲", "乙", "丙");
    private static final List<String> CATEGORIES =
            List.of("西药", "中成药", "中草药", "诊疗项目", "医用耗材");

    private static final String[][] DRUG_WEST = {
            {"阿莫西林胶囊", "0.25g*24粒", "盒"}, {"头孢克肟分散片", "0.1g*6片", "盒"},
            {"氯化钠注射液", "250ml", "瓶"}, {"葡萄糖注射液", "500ml", "瓶"},
            {"奥美拉唑肠溶胶囊", "20mg*14粒", "盒"}, {"甲硝唑片", "0.2g*21片", "盒"},
            {"布洛芬缓释胶囊", "0.3g*20粒", "盒"}, {"左氧氟沙星滴眼液", "5ml", "支"}};
    private static final String[][] DRUG_CN = {
            {"连花清瘟胶囊", "0.35g*24粒", "盒"}, {"感冒清热颗粒", "12g*10袋", "盒"},
            {"复方丹参滴丸", "27mg*180丸", "瓶"}, {"云南白药气雾剂", "85g+30g", "盒"}};
    private static final String[][] DRUG_HERB = {
            {"炙甘草", "g", "g"}, {"黄芪", "g", "g"}, {"党参", "g", "g"}};
    private static final String[][] TREAT = {
            {"普通门诊诊查费", "", "次"}, {"专家门诊诊查费", "", "次"},
            {"血常规", "", "次"}, {"胸部正位片", "", "次"},
            {"腹部彩超", "", "次"}, {"心电图", "", "次"},
            {"静脉输液", "", "次"}, {"中频脉冲电治疗", "", "次"}};
    private static final String[][] MATERIAL = {
            {"一次性注射器", "5ml", "支"}, {"输液器", "", "套"},
            {"医用敷贴", "", "片"}, {"采血管", "", "支"}};
    private static final String[] HOSP_ITEMS = {
            "门诊诊查费-普通", "门诊诊查费-专家", "血常规(五分类)", "胸部DR", "腹部彩色多普勒",
            "十二通道心电图", "静脉输液(第一组)", "中频脉冲电治疗", "阿莫西林胶囊", "头孢克肟分散片",
            "氯化钠注射液(塑瓶)", "奥美拉唑肠溶胶囊", "连花清瘟胶囊", "布洛芬缓释胶囊", "一次性注射器5ml",
            "一次性输液器", "医用无菌敷贴", "真空采血管"};

    /**
     * 重点关注参保人：同一身份证短期内多次结算，用于触发「高频就医 / 分解住院 / 挂床住院」等
     * 事前提醒与大数据反欺诈模型的真实命中。
     */
    private static final String[] HOT_PATIENT_NAME = {"李建国", "王秀英"};
    private static final String[] HOT_PATIENT_ID = {"430102197508123456", "430103198203154321"};

    private List<MisCatalog> catalogs = new ArrayList<>();
    private final List<SeedBill> seedBills = new ArrayList<>();

    @Override
    public void run(ApplicationArguments args) {
        // 进销存播种独立执行：它是后加的模块，不能被「目录非空则整体跳过」的守卫挡住，
        // 否则存量库永远补不上库存主档，盘点与结算出库都无从验证。
        seedStock();

        if (catalogMapper.selectCount(null) > 0) {
            return;
        }
        seedCatalog();
        seedMapping();
        seedBills();
        seedPending();
        seedAudit();
        seedFilings();
        seedRecon();
        log.info("[经办平台] 演示数据初始化完成：目录{} 对照{} 结算单{} 待结算{} 审核{} 备案{} 对账{} 库存{}",
                catalogs.size(), HOSP_ITEMS.length, seedBills.size(),
                pendingMapper.selectCount(null), auditMapper.selectCount(null),
                filingMapper.selectCount(null), reconMapper.selectCount(null),
                stockItemMapper.selectCount(null));
    }

    // --------------------------------------------------------------- 进销存

    /**
     * 药品耗材库存铺底：以目录中的药品/耗材为库存主档，灌入一条期初采购入库流水。
     * 库存数量与均价按目录限价折算，保证结算出库时能匹配到库存并正常扣减。
     */
    private void seedStock() {
        if (stockItemMapper.selectCount(null) != null && stockItemMapper.selectCount(null) > 0) {
            return;
        }
        // 直接查库取目录（不能依赖内存 catalogs：存量库跳过播种时 catalogs 为空）
        List<MisCatalog> src = catalogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MisCatalog>()
                        .in(MisCatalog::getCategory, List.of("西药", "中成药", "中草药", "医用耗材")));
        if (src.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        int seq = 0;
        for (MisCatalog c : src) {
            if (!"西药".equals(c.getCategory()) && !"中成药".equals(c.getCategory())
                    && !"中草药".equals(c.getCategory()) && !"医用耗材".equals(c.getCategory())) {
                continue;
            }
            seq++;
            MisStockItem item = new MisStockItem();
            item.setItemCode(String.format("YP%04d", seq));
            item.setItemName(c.getName());
            item.setCategory(c.getCategory());
            item.setSpec(c.getSpec());
            item.setUnit(c.getUnit());
            item.setYbCode(c.getYbCode());
            item.setManufacturer("演示生产企业-" + c.getCategory() + "股份有限公司");
            item.setLocation(c.getCategory().equals("医用耗材") ? "耗材库-02区" : "药品库-01区");
            item.setStatus(1);
            item.setStockQty(BigDecimal.ZERO);
            item.setStockAmount(BigDecimal.ZERO);
            item.setAvgPrice(BigDecimal.ZERO);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            stockItemMapper.insert(item);

            // 期初采购入库：数量按类别给出，售价取限价的 70%~90%
            double limit = c.getLimitPrice() == null ? 10.0 : c.getLimitPrice().doubleValue();
            BigDecimal price = bd(limit * (0.7 + rnd() * 0.2));
            int qty = c.getCategory().equals("医用耗材") ? randInt(200, 800)
                    : c.getCategory().equals("中草药") ? randInt(300, 1200) : randInt(80, 400);
            BigDecimal qtyBd = BigDecimal.valueOf(qty);
            BigDecimal amount = price.multiply(qtyBd).setScale(2, RoundingMode.HALF_UP);

            MisStockTxn t = new MisStockTxn();
            t.setTxnNo("KC" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + String.format("%05d", seq));
            t.setItemId(item.getId());
            t.setItemCode(item.getItemCode());
            t.setItemName(item.getItemName());
            t.setTxnType(1);
            t.setQty(qtyBd);
            t.setPrice(price);
            t.setAmount(amount);
            t.setBeforeQty(BigDecimal.ZERO);
            t.setAfterQty(qtyBd);
            t.setBatchNo("B" + today.format(DateTimeFormatter.ofPattern("yyyyMM")) + String.format("%03d", seq));
            t.setExpireDate(today.plusMonths(randInt(12, 30)));
            t.setRefNo("RK" + today.format(DateTimeFormatter.ofPattern("yyyyMM")) + String.format("%04d", seq));
            t.setBizDate(today.minusDays(randInt(20, 60)));
            t.setOperator("采购管理员");
            t.setRemark("期初采购入库");
            t.setCreateTime(LocalDateTime.now());
            stockTxnMapper.insert(t);

            item.setStockQty(qtyBd);
            item.setStockAmount(amount);
            item.setAvgPrice(price);
            item.setWarnQty(BigDecimal.valueOf(randInt(30, 120)));
            item.setUpdateTime(LocalDateTime.now());
            stockItemMapper.updateById(item);
        }
    }

    // --------------------------------------------------------------- 目录

    private void seedCatalog() {
        addCatalog("西药", DRUG_WEST, "X");
        addCatalog("中成药", DRUG_CN, "Z");
        addCatalog("中草药", DRUG_HERB, "ZYY");
        addCatalog("诊疗项目", TREAT, "L");
        addCatalog("医用耗材", MATERIAL, "H");
    }

    private void addCatalog(String category, String[][] items, String prefix) {
        boolean drug = category.equals("西药") || category.equals("中成药") || category.equals("中草药");
        for (String[] it : items) {
            String cls = pick(PAY_CLASS);
            BigDecimal selfRatio = new BigDecimal(cls.equals("甲") ? "0" : cls.equals("乙") ? "0.1" : "1");
            BigDecimal insRatio = new BigDecimal(cls.equals("甲") ? "0.85" : cls.equals("乙") ? "0.8" : "0");
            int base = randInt(drug ? 6 : 8, drug ? 320 : 460);
            BigDecimal limit = bd(base * (cls.equals("丙") ? 1.0 : randInt(8, 12) / 10.0));

            MisCatalog c = new MisCatalog();
            c.setYbCode(prefix + String.format("%09d", catalogs.size() + 1));
            c.setName(it[0]);
            c.setCategory(category);
            c.setSpec(it[1]);
            c.setUnit(it[2]);
            c.setPayClass(cls);
            c.setSelfRatio(selfRatio);
            c.setInsuranceRatio(insRatio);
            c.setLimitPrice(limit);
            c.setStatus(rnd() > 0.08 ? 1 : 0);
            catalogMapper.insert(c);
            catalogs.add(c);
        }
    }

    // --------------------------------------------------------------- 对照

    private void seedMapping() {
        for (int i = 0; i < HOSP_ITEMS.length; i++) {
            String name = HOSP_ITEMS[i];
            boolean mapped = rnd() > 0.25;
            MisCatalog c = mapped ? pick(catalogs) : null;
            String category = name.contains("费") ? "诊疗项目"
                    : (name.contains("注射") || name.contains("输液") || name.contains("敷贴") || name.contains("采血"))
                    ? "医用耗材" : "药品";
            MisItemMapping m = new MisItemMapping();
            m.setHospItemCode("H" + (1000 + i));
            m.setHospItemName(name);
            m.setCategory(category);
            m.setMapped(mapped);
            m.setYbCode(c == null ? "" : c.getYbCode());
            m.setYbName(c == null ? "" : c.getName());
            m.setPayClass(c == null ? "" : c.getPayClass());
            m.setUpdatedAt(LocalDateTime.now().minusDays(randInt(1, 12))
                    .withHour(randInt(8, 18)).withMinute(randInt(0, 59)).withSecond(0).withNano(0));
            mappingMapper.insert(m);
        }
    }

    // --------------------------------------------------------------- 结算单

    private void seedBills() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 42; i++) {
            String insType = pick(INSURANCE_TYPES);
            String settleType = pick(SETTLE_TYPES);
            List<MisFeeItem> items = genItems(randInt(3, 8));
            DecVO d = settleEngine.compute(items, insType);
            LocalDate day = today.minusDays(randInt(0, 9));
            LocalDateTime time = day.atTime(randInt(8, 18), randInt(0, 59));
            String status = pick(List.of("已结算", "已结算", "已结算", "待审核", "异常", "已退费"));
            String name = makeName();
            String idCard = makeIdCard();
            Integer admitDays = null;

            // ---- 构造可被监管模型真实命中的业务特征 ----
            if (i < 8) {
                // 同一参保人短期内多次结算（命中高频就医）
                name = HOT_PATIENT_NAME[0];
                idCard = HOT_PATIENT_ID[0];
                day = today.minusDays(randInt(0, 4));
                time = day.atTime(randInt(8, 18), randInt(0, 59));
            }
            if (i < 4) {
                // 住院天数长但费用畸低（命中挂床住院 / 分解住院 / 空刷医保卡）
                settleType = "住院";
                admitDays = randInt(12, 19);
                day = today.minusDays(randInt(0, 4));
                time = day.atTime(randInt(8, 18), randInt(0, 59));
                d.setTotal(bd(280 + randInt(0, 160)));
                d.setInsurancePay(BigDecimal.ZERO);
                d.setPersonalAccount(BigDecimal.ZERO);
                d.setCashPay(d.getTotal());
                d.setSelfPay(d.getTotal());
                scaleItems(items, d.getTotal());
            }
            if (i == 8) {
                // 部分项目未做医保目录对照（命中目录未对照 / 串换药品嫌疑）
                for (int k = 0; k < Math.min(2, items.size()); k++) {
                    items.get(k).setYbCode("");
                }
            }

            MisBill bill = new MisBill();
            bill.setBillNo("YL" + day.getYear() + String.format("%02d", day.getMonthValue()) + randInt(10000, 99999));
            bill.setBizNo(bill.getBillNo());
            bill.setPatientName(name);
            bill.setIdCard(idCard);
            bill.setIdCardMask(maskId(idCard));
            bill.setInsuranceType(insType);
            bill.setSettleType(settleType);
            bill.setDept(pick(DEPTS));
            bill.setDoctor(pick(DOCTORS));
            bill.setSettleTime(time);
            bill.setStatus(status);
            bill.setAdmitDays(admitDays);
            bill.setTotalAmount(d.getTotal());
            bill.setInsurancePay(d.getInsurancePay());
            bill.setPersonalAccount(d.getPersonalAccount());
            bill.setCashPay(d.getCashPay());
            bill.setSelfPay(d.getSelfPay());
            bill.setDeductible(d.getDeductible());
            bill.setBasePool(d.getBasePool());
            billMapper.insert(bill);

            for (MisFeeItem it : items) {
                it.setOwnerType(2);
                it.setOwnerId(bill.getId());
                if (status.equals("已退费")) {
                    it.setReversed(true);
                }
                feeItemMapper.insert(it);
            }
            seedBills.add(new SeedBill(bill, items, status));
        }
    }

    // --------------------------------------------------------------- 待结算

    private void seedPending() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 14; i++) {
            MisPendingSettle p = basePending(pick(INSURANCE_TYPES), "MZ" + (20261000 + i), 1);
            p.setBizTime(today.minusDays(randInt(0, 9)).atTime(randInt(8, 17), randInt(0, 59)));
            pendingMapper.insert(p);
            for (MisFeeItem it : genItems(randInt(3, 7))) {
                it.setOwnerType(1);
                it.setOwnerId(p.getId());
                feeItemMapper.insert(it);
            }
        }
        for (int i = 0; i < 10; i++) {
            MisPendingSettle p = basePending(pick(INSURANCE_TYPES), "ZY" + (20265000 + i), 2);
            p.setAdmitDays(randInt(3, 21));
            p.setBizTime(today.minusDays(randInt(0, 9)).atTime(randInt(8, 17), randInt(0, 59)));
            pendingMapper.insert(p);
            for (MisFeeItem it : genItems(randInt(6, 14))) {
                it.setOwnerType(1);
                it.setOwnerId(p.getId());
                feeItemMapper.insert(it);
            }
        }
    }

    private MisPendingSettle basePending(String insType, String bizNo, int bizType) {
        String idCard = makeIdCard();
        MisPendingSettle p = new MisPendingSettle();
        p.setBizType(bizType);
        p.setBizNo(bizNo);
        p.setPatientName(makeName());
        p.setIdCard(idCard);
        p.setIdCardMask(maskId(idCard));
        p.setInsuranceType(insType);
        p.setDept(pick(DEPTS));
        p.setDoctor(pick(DOCTORS));
        p.setStatus(1);
        return p;
    }

    // --------------------------------------------------------------- 审核

    private void seedAudit() {
        for (SeedBill sb : seedBills) {
            if (!sb.status.equals("待审核") && !sb.status.equals("异常")) {
                continue;
            }
            for (MisFeeItem it : sb.items) {
                if (("乙".equals(it.getPayClass()) || "丙".equals(it.getPayClass())) && rnd() > 0.4) {
                    MisFeeAudit a = new MisFeeAudit();
                    a.setBillId(sb.bill.getId());
                    a.setBillNo(sb.bill.getBillNo());
                    a.setPatientName(sb.bill.getPatientName());
                    a.setItemName(it.getName());
                    a.setCategory(it.getCategory());
                    a.setPayClass(it.getPayClass());
                    a.setAmount(it.getAmount());
                    a.setReason("丙".equals(it.getPayClass())
                            ? "丙类项目全自费，需确认患者知情同意"
                            : "乙类项目需个人先行自付部分费用");
                    a.setStatus("待审核");
                    auditMapper.insert(a);
                }
            }
        }
    }

    // --------------------------------------------------------------- 备案

    private void seedFilings() {
        List<String> areas = List.of("北京市", "上海市", "广州市", "深圳市", "武汉市", "成都市", "杭州市");
        List<String> reasons = List.of("异地长期居住", "异地转诊就医", "常驻异地工作");
        List<String> insAreas = List.of("长沙市", "株洲市", "湘潭市");
        List<String> hospitals = List.of("长沙市麓康医院", "湘雅医院", "湖南省人民医院");
        for (int i = 0; i < 12; i++) {
            MisFiling f = new MisFiling();
            f.setName(makeName());
            f.setIdCardMask(maskId(makeIdCard()));
            f.setInsuranceArea(pick(insAreas));
            f.setHospital(pick(hospitals));
            f.setReason(pick(reasons));
            f.setFromArea(pick(areas));
            f.setValidStart(LocalDate.of(2026, randInt(1, 9), randInt(1, 9)));
            f.setValidEnd(LocalDate.of(2026, randInt(10, 12), randInt(10, 28)));
            f.setStatus(pick(List.of("已备案", "审核中", "已失效")));
            f.setApplyTime(LocalDate.now().minusDays(randInt(0, 11)));
            filingMapper.insert(f);
        }
    }

    // --------------------------------------------------------------- 对账

    private void seedRecon() {
        LocalDate today = LocalDate.now();
        for (int i = 9; i >= 1; i--) {
            LocalDate m = today.minusMonths(i);
            String period = m.getYear() + "-" + String.format("%02d", m.getMonthValue());
            int billCount = randInt(800, 2600);
            BigDecimal insurancePay = bd(billCount * randInt(280, 520));
            BigDecimal received = bd(insurancePay.doubleValue() * (0.97 + rnd() * 0.05));
            BigDecimal diff = received.subtract(insurancePay);
            MisRecon r = new MisRecon();
            r.setPeriod(period);
            r.setBillCount(billCount);
            r.setInsurancePay(insurancePay);
            r.setSettleAmount(bd(insurancePay.doubleValue() / (rnd() * 0.3 + 0.5)));
            r.setReceivedFromFund(received);
            r.setDiff(diff);
            r.setStatus(i == 1 ? "待对账" : (diff.abs().compareTo(BigDecimal.ONE) < 0 ? "已对账" : "存在差异"));
            reconMapper.insert(r);
        }
    }

    // --------------------------------------------------------------- 工具

    private List<MisFeeItem> genItems(int n) {
        List<MisFeeItem> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            MisCatalog c = pick(catalogs);
            int qty = c.getCategory().equals("中草药") ? randInt(50, 200) : randInt(1, 6);
            BigDecimal price = bd(c.getLimitPrice().doubleValue() * (0.6 + rnd() * 0.6));
            MisFeeItem it = new MisFeeItem();
            it.setName(c.getName());
            it.setCategory(c.getCategory());
            it.setSpec(c.getSpec());
            it.setUnit(c.getUnit());
            it.setYbCode(c.getYbCode());
            it.setPayClass(c.getPayClass());
            it.setSelfRatio(c.getSelfRatio());
            it.setInsuranceRatio(c.getInsuranceRatio());
            it.setQty(qty);
            it.setPrice(price);
            it.setAmount(price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP));
            it.setReversed(false);
            list.add(it);
        }
        return list;
    }

    private String makeName() {
        String s = "" + SURNAMES.charAt((int) (rnd() * SURNAMES.length()))
                + GIVEN.charAt((int) (rnd() * GIVEN.length()));
        if (rnd() > 0.5) {
            s += GIVEN.charAt((int) (rnd() * GIVEN.length()));
        }
        return s;
    }

    private String makeIdCard() {
        String region = "4301" + randInt(10, 99) + randInt(10, 99);
        return region + randInt(1960, 2003) + String.format("%02d", randInt(1, 12))
                + String.format("%02d", randInt(1, 28)) + String.format("%04d", randInt(0, 9999));
    }

    private String maskId(String id) {
        return id.substring(0, 6) + "********" + id.substring(id.length() - 4);
    }

    private double rnd() {
        seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
        return seed / (double) Integer.MAX_VALUE;
    }

    private <T> T pick(List<T> arr) {
        return arr.get((int) (rnd() * arr.size()));
    }

    private int randInt(int a, int b) {
        return a + (int) (rnd() * (b - a + 1));
    }

    /** 按比例缩放费用明细，使明细合计与结算单金额保持一致（构造低费用住院等特征用） */
    private void scaleItems(List<MisFeeItem> items, BigDecimal target) {
        BigDecimal sum = BigDecimal.ZERO;
        for (MisFeeItem it : items) {
            sum = sum.add(it.getAmount() == null ? BigDecimal.ZERO : it.getAmount());
        }
        if (sum.compareTo(BigDecimal.ZERO) <= 0 || target == null) {
            return;
        }
        BigDecimal ratio = target.divide(sum, 6, RoundingMode.HALF_UP);
        for (MisFeeItem it : items) {
            BigDecimal amt = (it.getAmount() == null ? BigDecimal.ZERO : it.getAmount()).multiply(ratio)
                    .setScale(2, RoundingMode.HALF_UP);
            it.setAmount(amt);
            if (it.getQty() != null && it.getQty() > 0) {
                it.setPrice(amt.divide(BigDecimal.valueOf(it.getQty()), 2, RoundingMode.HALF_UP));
            }
        }
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private record SeedBill(MisBill bill, List<MisFeeItem> items, String status) {
    }
}
