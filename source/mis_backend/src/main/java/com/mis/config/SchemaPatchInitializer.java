package com.mis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 存量库结构补丁（启动时执行，幂等）：schema.sql 全部 IF NOT EXISTS，只对新建库生效；
 * 已存在的表加列走这里，列已存在（Duplicate column）静默跳过。
 * <p>另含表名规范化迁移：老库表名为 mss_ 前缀，按域拆分为
 * mis_（医保结算经办）/ mif_（医保基金监管）/ sys_（平台公共）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(1)
public class SchemaPatchInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    /** 老表名 → 新表名（mis_ 结算经办 / mif_ 基金监管 / sys_ 平台公共） */
    private static final String[][] TABLE_RENAMES = {
            // ---- 医保结算经办 mis_ ----
            {"mss_insured_person", "mis_insured_person"}, {"mss_settlement", "mis_settlement"},
            {"mss_message", "mis_message"}, {"mss_monthly", "mis_monthly"},
            {"mss_monthly_deduct", "mis_monthly_deduct"}, {"mss_allocation", "mis_allocation"},
            {"mss_catalog", "mis_catalog"}, {"mss_item_mapping", "mis_item_mapping"},
            {"mss_pending_settle", "mis_pending_settle"}, {"mss_bill", "mis_bill"},
            {"mss_fee_item", "mis_fee_item"}, {"mss_fee_audit", "mis_fee_audit"},
            {"mss_filing", "mis_filing"}, {"mss_recon", "mis_recon"},
            {"mss_daily_alloc", "mis_daily_alloc"}, {"mss_agreement", "mis_agreement"},
            {"mss_manual_claim", "mis_manual_claim"}, {"mss_transfer", "mis_transfer"},
            {"mss_account_txn", "mis_account_txn"}, {"mss_chronic", "mis_chronic"},
            {"mss_stock_item", "mis_stock_item"}, {"mss_stock_txn", "mis_stock_txn"},
            // ---- 医保基金监管 mif_ ----
            {"mss_audit_rule", "mif_audit_rule"}, {"mss_suspicion", "mif_suspicion"},
            {"mss_inspection", "mif_inspection"}, {"mss_inspection_item", "mif_inspection_item"},
            {"mss_recover", "mif_recover"}, {"mss_knowledge", "mif_knowledge"},
            {"mss_credit", "mif_credit"}, {"mss_fraud_model", "mif_fraud_model"},
            {"mss_fraud_clue", "mif_fraud_clue"}, {"mss_precheck_rule", "mif_precheck_rule"},
            {"mss_precheck_result", "mif_precheck_result"}, {"mss_report", "mif_report"},
            {"mss_drg_group", "mif_drg_group"}, {"mss_drg_case", "mif_drg_case"},
            {"mss_random_task", "mif_random_task"}, {"mss_random_result", "mif_random_result"},
            {"mss_self_check", "mif_self_check"}, {"mss_quality_rule", "mif_quality_rule"},
            {"mss_quality_result", "mif_quality_result"}, {"mss_trace_code", "mif_trace_code"},
            {"mss_trace_verify_log", "mif_trace_verify_log"},
            // ---- 平台公共 sys_ ----
            {"mss_department", "sys_department"}, {"mss_role", "sys_role"},
            {"mss_menu", "sys_menu"}, {"mss_role_menu", "sys_role_menu"},
            {"mss_user", "sys_user"}, {"mss_employee", "sys_employee"},
            {"mss_employee_post", "sys_employee_post"}, {"mss_employee_qualification", "sys_employee_qualification"},
            {"mss_config", "sys_config"}, {"mss_dict_type", "sys_dict_type"},
            {"mss_dict_data", "sys_dict_data"},
    };

    @Override
    public void run(ApplicationArguments args) {
        migrateTableNames();
        migrateAuditColumns();
        migrateMenuTree();
        patch("mis_settlement 加 2301 个账实扣列（撤销冲正退回个账用）",
                "ALTER TABLE mis_settlement ADD COLUMN account_deduct DECIMAL(12,2) DEFAULT 0 "
                        + "COMMENT '2301个账实扣（撤销冲正退回个账）'");
        patch("mis_fee_audit 加申诉/复审/合议列（智能审核闭环）",
                "ALTER TABLE mis_fee_audit ADD COLUMN appeal_reason VARCHAR(255) DEFAULT NULL COMMENT '申诉理由', "
                        + "ADD COLUMN review_result VARCHAR(16) DEFAULT NULL COMMENT '复审结论（复审通过/复审维持）', "
                        + "ADD COLUMN review_opinion VARCHAR(255) DEFAULT NULL COMMENT '复审意见', "
                        + "ADD COLUMN confer_opinion VARCHAR(255) DEFAULT NULL COMMENT '合议意见（终裁）'");
        patch("mis_insured_person 加停保/恢复原因列（参保状态变更留痕）",
                "ALTER TABLE mis_insured_person ADD COLUMN status_remark VARCHAR(255) DEFAULT NULL COMMENT '停保/恢复原因留痕'");
        patch("mif_precheck_rule 加规则参数列（事前/事中规则可配置阈值）",
                "ALTER TABLE mif_precheck_rule ADD COLUMN params TEXT COMMENT '规则参数（JSON）'");
        patch("mif_precheck_result 加风险等级/更新时间列",
                "ALTER TABLE mif_precheck_result ADD COLUMN risk_level INT DEFAULT 2 COMMENT '风险等级（1-低 2-中 3-高）', "
                        + "ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
    }

    /**
     * 表名规范化迁移：老库存在 mss_ 前缀表时改名为 mis_/mif_/sys_（幂等，可重复执行）。
     */
    private void migrateTableNames() {
        int renamed = 0;
        int skipped = 0;
        for (String[] pair : TABLE_RENAMES) {
            String oldName = pair[0];
            String newName = pair[1];
            try {
                Integer oldCount = tableCount(oldName);
                if (oldCount == null || oldCount == 0) {
                    skipped++;
                    continue;
                }
                if (tableCount(newName) != null && tableCount(newName) > 0) {
                    // 新表已存在（迁移执行过），直接删旧表避免残留
                    jdbcTemplate.execute("DROP TABLE IF EXISTS " + oldName);
                    log.info("[表名迁移] {} → {}：新表已存在，丢弃旧表", oldName, newName);
                    renamed++;
                    continue;
                }
                jdbcTemplate.execute("RENAME TABLE " + oldName + " TO " + newName);
                log.info("[表名迁移] {} → {}：已改名", oldName, newName);
                renamed++;
            } catch (Exception e) {
                log.warn("[表名迁移] {} → {}：执行失败 {}", oldName, newName, e.getMessage());
            }
        }
        log.info("[表名迁移] 完成：改名 {} 张，跳过（不存在）{} 张", renamed, skipped);
    }

    private Integer tableCount(String table) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT COUNT(*) AS c FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                    table);
            if (rows == null || rows.isEmpty()) {
                return 0;
            }
            Object c = rows.get(0).get("c");
            return c == null ? 0 : Integer.parseInt(String.valueOf(c));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 标准审计字段补齐：检查库内所有表的 6 个审计列，缺哪个补哪个（幂等）。
     * <p>列定义：create_by_id / create_by / create_time / update_by_id / update_by / update_time。
     */
    private void migrateAuditColumns() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'", String.class);
        if (tables == null || tables.isEmpty()) {
            return;
        }
        int fixed = 0;
        for (String t : tables) {
            List<String> cols = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = ?", String.class, t);
            List<String> exist = cols == null ? List.of() : cols;
            StringBuilder alter = new StringBuilder();
            for (String[] c : AUDIT_COLUMN_DDL) {
                if (!exist.contains(c[0])) {
                    alter.append(alter.length() > 0 ? ", " : "").append(c[1]);
                }
            }
            if (alter.length() == 0) {
                continue;
            }
            try {
                jdbcTemplate.execute("ALTER TABLE " + t + " " + alter);
                log.info("[审计字段] {}：补齐 {}", t, alter);
                fixed++;
            } catch (Exception e) {
                log.warn("[审计字段] {}：补齐失败 {}", t, e.getMessage());
            }
        }
        log.info("[审计字段] 完成：{} 张表已补齐", fixed);
    }

    /** 标准审计列定义：{列名, ALTER 片段} */
    private static final String[][] AUDIT_COLUMN_DDL = {
            {"create_by_id", "ADD COLUMN create_by_id BIGINT DEFAULT NULL COMMENT '创建人ID'"},
            {"create_by", "ADD COLUMN create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人姓名'"},
            {"create_time", "ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'"},
            {"update_by_id", "ADD COLUMN update_by_id BIGINT DEFAULT NULL COMMENT '修改人ID'"},
            {"update_by", "ADD COLUMN update_by VARCHAR(64) DEFAULT NULL COMMENT '修改人姓名'"},
            {"update_time", "ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'"},
    };

    private void patch(String desc, String ddl) {
        try {
            jdbcTemplate.execute(ddl);
            log.info("[结构补丁] {}：已执行", desc);
        } catch (Exception e) {
            if (String.valueOf(e.getMessage()).contains("Duplicate column")) {
                log.info("[结构补丁] {}：列已存在，跳过", desc);
                return;
            }
            log.warn("[结构补丁] {}：执行失败 {}", desc, e.getMessage());
        }
    }

    // ==================================================================
    // 菜单三级化迁移
    // ==================================================================

    /**
     * 菜单结构三级化：顶级只有三个业务域（医保结算经办 / 医保基金监管 / 系统管理），
     * 二级为业务分组，三级为页面。
     * <p>存量库原来把目录与页面混挂在顶级（工作台、医保目录管理、清算对账…直接 parent_id=0），
     * 且基金监管 15 个菜单挤在一层，用户分不清归属。本方法把结构拉平重排，幂等可重复执行。
     * <p>关键约束：三级页面的 path 必须与前端路由一致，否则菜单点不开；
     * 页面的 parent_id 变更后，角色授权要补上新的祖先链，否则菜单树挂不上。
     */
    /** 顶级：{id, 名称, menu_key, 图标, 排序} */
    private static final Object[][] MENU_TOPS = {
            {2L, "医保结算经办", "settle", "Briefcase", 10},
            {13L, "医保基金监管", "supervise", "Monitor", 20},
            {9L, "系统管理", "system", "Setting", 30},
    };

    /** 二级分组：{id, 名称, 顶级id, menu_key, 图标, 权限码} */
    private static final Object[][] MENU_GROUPS = {
            {200L, "结算经办", 2L, "group.settle.do", "CreditCard", "settle:do:list"},
            {201L, "结算与清算", 2L, "group.settle.fin", "Money", "settle:fin:list"},
            {202L, "参保与账户", 2L, "group.settle.insured", "User", "insured:person:list"},
            {203L, "费用与目录", 2L, "group.settle.catalog", "Notebook", "catalog:list"},
            {204L, "待遇与备案", 2L, "group.settle.filing", "Stamp", "filing:list"},
            {205L, "进销存", 2L, "group.settle.stock", "Goods", "stock:item:list"},
            {206L, "经办看板", 2L, "group.settle.board", "DataBoard", "settle:board:view"},
            {210L, "监管总览", 13L, "group.supervise.overview", "DataBoard", "supervise:dashboard:view"},
            {211L, "规则与知识", 13L, "group.supervise.rule", "SetUp", "supervise:rule:list"},
            {212L, "稽核核查", 13L, "group.supervise.audit", "Search", "supervise:suspicion:list"},
            {213L, "智能监管", 13L, "group.supervise.ai", "Aim", "supervise:fraud:list"},
            {214L, "专项监管", 13L, "group.supervise.special", "Histogram", "supervise:quality:list"},
            {215L, "信用管理", 13L, "group.supervise.credit", "Medal", "supervise:credit:list"},
            {220L, "用户与权限", 9L, "group.system.user", "User", "system:user:list"},
            {221L, "组织与岗位", 9L, "group.system.org", "OfficeBuilding", "system:dept:list"},
            {222L, "基础配置", 9L, "group.system.dict", "Collection", "system:dict:list"},
            {223L, "审计与待办", 9L, "group.system.audit", "Document", "system:operlog:list"},
    };

    /** 三级页面：{页面id, 所属二级分组id} */
    private static final Object[][] MENU_PAGES = {
            // 经办侧
            {1L, 206L}, {21L, 200L}, {22L, 200L}, {24L, 200L},
            {23L, 201L}, {150L, 201L}, {25L, 201L}, {26L, 201L}, {7L, 201L},
            {31L, 202L}, {32L, 202L},
            {4L, 203L}, {5L, 203L}, {151L, 203L}, {10L, 203L},
            {11L, 204L}, {6L, 204L},
            {33L, 205L}, {152L, 205L},
            {8L, 206L},
            // 监管侧
            {131L, 210L}, {140L, 210L},
            {132L, 211L}, {136L, 211L},
            {133L, 212L}, {134L, 212L}, {135L, 212L},
            {139L, 213L}, {138L, 213L}, {141L, 213L},
            {144L, 214L}, {145L, 214L}, {142L, 214L}, {143L, 214L},
            {137L, 215L},
            // 系统侧
            {91L, 220L}, {92L, 220L}, {93L, 220L},
            {94L, 221L}, {96L, 221L}, {97L, 221L},
            {95L, 222L},
            {153L, 223L}, {154L, 223L},
    };

    /** 按钮：{按钮id, 所属页面id} */
    private static final Object[][] MENU_BUTTONS = {
            {101L, 91L}, {102L, 91L}, {103L, 91L},
            {104L, 92L}, {105L, 92L},
            {106L, 93L}, {107L, 93L},
            {108L, 95L}, {109L, 95L}, {110L, 95L},
            {111L, 97L}, {112L, 97L},
    };

    /** 废弃的旧顶级目录（子菜单已下沉到二级分组） */
    private static final long[] MENU_OBSOLETE_TOPS = {3L};

    /**
     * 菜单图标修正表：{菜单id, 正确图标名}。
     * <p>图标名必须是 @element-plus/icons-vue 里存在的组件名，否则前端
     * {@code <component :is="icon">} 会渲染成一个空的自定义标签 —— 图标不显示、
     * 控制台也不报错，是最难被发现的一类问题。这里固化全量图标，存量库启动即纠正。
     * <p>校验脚本：scripts/check-menu-icons.js
     */
    private static final Object[][] MENU_ICONS = {
            // ---- 顶级 ----
            {2L, "Briefcase"}, {13L, "Monitor"}, {9L, "Setting"},
            // ---- 二级分组 ----
            {200L, "CreditCard"}, {201L, "Money"}, {202L, "User"}, {203L, "Notebook"},
            {204L, "Stamp"}, {205L, "Goods"}, {206L, "DataBoard"},
            {210L, "DataBoard"}, {211L, "SetUp"}, {212L, "Search"}, {213L, "Aim"},
            {214L, "Histogram"}, {215L, "Medal"},
            {220L, "User"}, {221L, "OfficeBuilding"}, {222L, "Collection"}, {223L, "Document"},
            // ---- 三级页面 ----
            {1L, "DataBoard"}, {21L, "Ticket"}, {22L, "FirstAidKit"}, {24L, "Upload"},
            {23L, "Document"}, {150L, "Tickets"}, {25L, "Calendar"}, {26L, "Wallet"},
            {7L, "DataAnalysis"},
            {31L, "UserFilled"}, {32L, "Switch"},
            {4L, "Stamp"}, {5L, "Notebook"}, {151L, "Collection"}, {10L, "Postcard"},
            {11L, "Money"}, {6L, "Tickets"},
            {33L, "Goods"}, {152L, "Files"},
            {8L, "TrendCharts"},
            {131L, "DataBoard"}, {140L, "ChatDotRound"}, {132L, "SetUp"}, {136L, "Reading"},
            {133L, "Search"}, {134L, "Van"}, {135L, "RefreshLeft"},
            {139L, "Bell"}, {138L, "Aim"}, {141L, "Histogram"},
            {144L, "DocumentChecked"}, {145L, "Postcard"}, {142L, "Refresh"}, {143L, "Finished"},
            {137L, "Medal"},
            {91L, "User"}, {92L, "Avatar"}, {93L, "Menu"},
            {94L, "Postcard"}, {96L, "OfficeBuilding"}, {97L, "Avatar"},
            {95L, "Collection"},
            {153L, "Document"}, {154L, "Bell"},
    };

    private void migrateMenuTree() {
        try {
            if (!tableExists("sys_menu")) {
                return;
            }
            // 1) 顶级收敛为三个业务域
            for (Object[] t : MENU_TOPS) {
                jdbcTemplate.update(
                        "UPDATE sys_menu SET menu_name=?, parent_id=0, sort_order=?, menu_type=1, "
                                + "path=?, component=NULL, menu_key=?, icon=?, permission=NULL WHERE id=?",
                        t[1], t[4], t[2], t[2], t[3], t[0]);
            }
            // 2) 二级分组（不存在则插入，已存在则更新）
            int newGroups = 0;
            for (Object[] g : MENU_GROUPS) {
                Integer exists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_menu WHERE id=?", Integer.class, g[0]);
                boolean isNew = exists == null || exists == 0;
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, menu_type, path, component, "
                                + "menu_key, icon, permission, is_visible, status) "
                                + "VALUES (?,?,?,?,1,?,NULL,?,?,?,1,1) "
                                + "ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), parent_id=VALUES(parent_id), "
                                + "sort_order=VALUES(sort_order), menu_type=1, path=VALUES(path), "
                                + "menu_key=VALUES(menu_key), icon=VALUES(icon), permission=VALUES(permission)",
                        g[0], g[1], g[2], ((Long) g[0]) % 100, g[2], g[3], g[4], g[5]);
                if (isNew) {
                    newGroups++;
                    log.info("[菜单迁移] 新建二级分组：{} ({})", g[1], g[0]);
                }
            }
            // 3) 三级页面下沉
            for (Object[] p : MENU_PAGES) {
                jdbcTemplate.update("UPDATE sys_menu SET parent_id=?, menu_type=2 WHERE id=?", p[1], p[0]);
            }
            // 4) 按钮跟随所属页面
            for (Object[] b : MENU_BUTTONS) {
                jdbcTemplate.update("UPDATE sys_menu SET parent_id=?, menu_type=3 WHERE id=?", b[1], b[0]);
            }
            // 5) 废弃旧顶级目录
            for (long id : MENU_OBSOLETE_TOPS) {
                jdbcTemplate.update("DELETE FROM sys_role_menu WHERE menu_id=?", id);
                jdbcTemplate.update("DELETE FROM sys_menu WHERE id=?", id);
            }
            // 5.1 图标纠正：图标名必须存在于 @element-plus/icons-vue，否则前端渲染成空白
            int fixedIcons = 0;
            for (Object[] mi : MENU_ICONS) {
                Long menuId = (Long) mi[0];
                String icon = (String) mi[1];
                Integer n = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_menu WHERE id=? AND icon=?",
                        Integer.class, menuId, icon);
                if (n == null || n == 0) {
                    jdbcTemplate.update("UPDATE sys_menu SET icon=? WHERE id=?", icon, menuId);
                    fixedIcons++;
                }
            }
            if (fixedIcons > 0) {
                log.info("[菜单迁移] 纠正无效图标 {} 处（图标名不存在于 @element-plus/icons-vue 会渲染成空白）",
                        fixedIcons);
            }
            // 6) 补齐角色授权：页面可见 ⇒ 其二级分组 + 顶级域都要授权
            int granted = 0;
            for (Object[] p : MENU_PAGES) {
                Long pageId = (Long) p[0];
                Long groupId = (Long) p[1];
                Long topId = null;
                for (Object[] g : MENU_GROUPS) {
                    if (g[0].equals(groupId)) {
                        topId = (Long) g[2];
                    }
                }
                if (topId == null) {
                    continue;
                }
                for (Long rid : new Long[]{1L, 2L, 3L}) {
                    // 页面本身已授权才补祖先（不越权授予页面）
                    if (!roleHasMenu(rid, pageId)) {
                        continue;
                    }
                    granted += grantIfAbsent(rid, groupId);
                    granted += grantIfAbsent(rid, topId);
                }
            }
            log.info("[菜单迁移] 三级结构就位：顶级 3 / 二级 {}（新建 {}）/ 页面 {} / 按钮 {}，补授权 {} 条",
                    MENU_GROUPS.length, newGroups, MENU_PAGES.length, MENU_BUTTONS.length, granted);
        } catch (Exception e) {
            log.warn("[菜单迁移] 执行失败（不影响启动）：{}", e.getMessage());
        }
    }

    /** 角色是否已拥有该菜单 */
    private boolean roleHasMenu(Long roleId, Long menuId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_menu WHERE role_id=? AND menu_id=?",
                Integer.class, roleId, menuId);
        return n != null && n > 0;
    }

    /** 补一条授权（已存在则跳过）；sys_role_menu.id 无自增，用最大 id+1 */
    private int grantIfAbsent(Long roleId, Long menuId) {
        if (roleHasMenu(roleId, menuId)) {
            return 0;
        }
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id),0) FROM sys_role_menu", Long.class);
        jdbcTemplate.update("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?,?,?)",
                (maxId == null ? 1000L : maxId + 1), roleId, menuId);
        return 1;
    }

    private boolean tableExists(String table) {
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",
                    Integer.class, table);
            return n != null && n > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
