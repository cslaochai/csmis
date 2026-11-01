import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * 旧表(mss_*) → 新表(mis_/mif_/sys_*) 数据迁移工具。
 * 按「公共列名」自动生成 INSERT ... SELECT，只迁新表存在且旧表也存在的列；
 * 新表独有的列（审计字段等）留空由 DEFAULT 填充。
 */
public class MigrateTool {

    static final String URL = System.getProperty("db.url",
            "jdbc:mysql://192.168.88.132:3306/mss_insurance?useUnicode=true&characterEncoding=utf8"
            + "&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
    static final String USER = System.getProperty("db.user", "xz_feng");
    static final String PASS = System.getProperty("db.pass", "Feng123456!");

    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            // 1. 找出所有旧表及其对应新表
            Map<String, String> pairs = new LinkedHashMap<>();
            for (String old : listTables(conn, "mss\\_%")) {
                String nw;
                if (Arrays.asList(AMBIGUOUS).contains(old)) {
                    nw = resolveAmbiguous(conn, old);
                } else {
                    nw = mapName(old);
                }
                if (nw != null && tableExists(conn, nw)) {
                    pairs.put(old, nw);
                } else {
                    System.out.println("NOMAP " + old + " (未找到目标表)");
                }
            }
            System.out.println("可迁移对: " + pairs.size());
            for (Map.Entry<String, String> e : pairs.entrySet()) {
                migrate(conn, e.getKey(), e.getValue());
            }
            System.out.println("== 迁移完成 ==");
        }
    }

    /** mss_xxx → mis_/mif_/sys_ 映射：按已知表名对照 */
    static final Map<String, String> EXPLICIT = new HashMap<>();

    static {
        // 经办
        String[] mis = {"insured_person", "settlement", "message", "monthly", "monthly_deduct", "allocation",
                "catalog", "item_mapping", "pending_settle", "bill", "fee_item", "fee_audit", "filing",
                "manual_claim", "agreement", "daily_alloc", "recon", "transfer", "account_txn", "chronic",
                "stock_item", "stock_txn"};
        for (String s : mis) EXPLICIT.put("mss_" + s, "mis_" + s);
        // 监管
        String[] mif = {"audit_rule", "suspicion", "inspection", "inspection_item", "recover",
                "precheck_rule", "precheck_result", "fraud_model", "fraud_clue", "quality_rule", "quality_result",
                "trace_code", "trace_verify_log", "drg_group", "drg_case", "random_task", "random_result",
                "self_check", "knowledge", "credit", "report"};
        for (String s : mif) EXPLICIT.put("mss_" + s, "mif_" + s);
        // 公共
        String[] sys = {"department", "role", "menu", "role_menu", "user", "employee", "employee_post",
                "employee_qualification", "config", "dict_type", "dict_data"};
        for (String s : sys) EXPLICIT.put("mss_" + s, "sys_" + s);
    }

    /** 两域都存在的表（按公共列占比择优） */
    static final String[] AMBIGUOUS = {"mss_precheck_rule", "mss_fraud_model", "mss_fraud_clue",
            "mss_knowledge", "mss_credit"};

    static String mapName(String old) {
        if (EXPLICIT.containsKey(old)) return EXPLICIT.get(old);
        return null;
    }

    /** 歧义表：选择公共列更多的目标域 */
    static String resolveAmbiguous(Connection conn, String old) throws SQLException {
        String base = old.substring(4);
        String best = null;
        int bestScore = -1;
        for (String prefix : new String[]{"mis_", "mif_"}) {
            String cand = prefix + base;
            if (!tableExists(conn, cand)) continue;
            Set<String> oc = columns(conn, old);
            Set<String> nc = columns(conn, cand);
            int score = 0;
            for (String c : oc) if (nc.contains(c)) score++;
            if (score > bestScore) { bestScore = score; best = cand; }
        }
        return best;
    }

    static void migrate(Connection conn, String oldT, String newT) throws Exception {
        Set<String> oldCols = columns(conn, oldT);
        Set<String> newCols = columns(conn, newT);
        List<String> common = new ArrayList<>();
        for (String c : oldCols) {
            if (newCols.contains(c)) common.add(c);
        }
        if (common.isEmpty()) {
            System.out.println("SKIP  " + oldT + " -> " + newT + " (无公共列)");
            return;
        }
        // id 主键冲突：新表为空则直接搬；否则跳过
        long newCnt = count(conn, newT);
        if (newCnt > 0) {
            System.out.println("SKIP  " + oldT + " -> " + newT + " (新表已有 " + newCnt + " 行)");
            return;
        }
        long oldCnt = count(conn, oldT);
        String cols = String.join(", ", common);
        String sql = "INSERT INTO " + newT + " (" + cols + ") SELECT " + cols + " FROM " + oldT;
        try (Statement st = conn.createStatement()) {
            int n = st.executeUpdate(sql);
            System.out.println("OK    " + oldT + " -> " + newT + "  迁移 " + n + " 行 (共 " + oldCnt + " 行, 列 " + common.size() + ")");
        } catch (SQLException e) {
            System.out.println("FAIL  " + oldT + " -> " + newT + " : " + e.getMessage().split("\n")[0]);
        }
    }

    static List<String> listTables(Connection conn, String like) throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE ?")) {
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        }
        return out;
    }

    static boolean tableExists(Connection conn, String t) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
            ps.setString(1, t);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static Set<String> columns(Connection conn, String t) throws SQLException {
        Set<String> out = new LinkedHashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=?")) {
            ps.setString(1, t);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        }
        return out;
    }

    static long count(Connection conn, String t) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + t)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
