import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * 通用 SQL 执行器（JDBC 直连）。
 * 用法:
 *   java DbTool.java <mode> [args...]
 *   mode=query  sql...        逐条执行并打印结果集
 *   mode=file   <sqlFile>     执行整个 .sql 文件（按 ; 切分语句，支持注释与 DELIMITER 不需要）
 *   mode=count                打印当前库表清单与行数统计
 *   mode=check                打印库连通性 + 表数量
 */
public class DbTool {

    static final String URL = System.getProperty("db.url",
            "jdbc:mysql://192.168.88.132:3306/mss_insurance?useUnicode=true&characterEncoding=utf8"
            + "&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai"
            + "&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true");
    static final String USER = System.getProperty("db.user", "xz_feng");
    static final String PASS = System.getProperty("db.pass", "Feng123456!");

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "check";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("== CONNECTED: " + conn.getCatalog() + " ==");
            switch (mode) {
                case "query" -> {
                    String sql = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    runScript(conn, sql, true);
                }
                case "file" -> {
                    String content = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);
                    runScript(conn, content, true);
                }
                case "count" -> {
                    runScript(conn, "SHOW TABLES;", true);
                    runScript(conn, "SELECT COUNT(*) AS tables_count FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE()", true);
                }
                case "check" -> {
                    runScript(conn, "SELECT COUNT(*) AS tables_count FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE()", true);
                }
                case "split" -> {
                    String content = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);
                    List<String> stmts = splitStatements(content);
                    System.out.println("语句数: " + stmts.size());
                    for (int i = 0; i < Math.min(4, stmts.size()); i++) {
                        String x = stmts.get(i);
                        System.out.println("--- [" + i + "] len=" + x.length() + " head=[" + x.substring(0, Math.min(60, x.length())).replace("\n", "\\n") + "]");
                    }
                }
                default -> System.out.println("unknown mode: " + mode);
            }
        }
    }

    /** 按 ; 切分语句，忽略行注释与块注释，逐条执行 */
    static void runScript(Connection conn, String script, boolean showResult) throws Exception {
        List<String> stmts = splitStatements(script);
        int ok = 0, fail = 0;
        for (String raw : stmts) {
            String sql = raw.trim();
            if (sql.isEmpty()) continue;
            try (Statement st = conn.createStatement()) {
                boolean hasRs = st.execute(sql);
                ok++;
                if (showResult && hasRs) {
                    try (ResultSet rs = st.getResultSet()) {
                        printResult(rs);
                    }
                } else if (showResult) {
                    System.out.println("OK  " + truncate(sql) + "  rows=" + st.getUpdateCount());
                }
            } catch (SQLException e) {
                fail++;
                System.out.println("FAIL  " + truncate(sql));
                System.out.println("      -> " + e.getMessage().split("\n")[0]);
            }
        }
        System.out.println("== statements: ok=" + ok + " fail=" + fail + " ==");
    }

    static String truncate(String s) {
        s = s.replaceAll("\\s+", " ");
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }

    static void printResult(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        StringBuilder head = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            if (i > 1) head.append(" | ");
            head.append(md.getColumnLabel(i));
        }
        System.out.println(head);
        int rows = 0;
        while (rs.next()) {
            if (rows++ > 200) { System.out.println("... (truncated)"); break; }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= n; i++) {
                if (i > 1) sb.append(" | ");
                Object v = rs.getObject(i);
                String t = v == null ? "NULL" : String.valueOf(v);
                if (t.length() > 60) t = t.substring(0, 60) + "..";
                sb.append(t);
            }
            System.out.println(sb);
        }
        System.out.println("(" + rows + " rows)");
    }

    /** 状态机切分 SQL：处理字符串字面量、注释、DELIMITER */
    static List<String> splitStatements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingle = false, inDouble = false, inBacktick = false;
        boolean inLineComment = false, inBlockComment = false;
        String[] lines = script.split("\r?\n", -1);
        for (String line : lines) {
            if (inBlockComment) {
                int end = line.indexOf("*/");
                if (end < 0) continue;
                line = line.substring(end + 2);
                inBlockComment = false;
            }
            // 处理 DELIMITER
            String trimmed = line.trim();
            if (trimmed.toUpperCase(Locale.ROOT).startsWith("DELIMITER ")) {
                if (!cur.toString().trim().isEmpty()) { out.add(cur.toString()); cur.setLength(0); }
                continue;
            }
            int i = 0;
            while (i < line.length()) {
                char ch = line.charAt(i);
                if (!inSingle && !inDouble && !inBacktick) {
                    if (ch == '-' && i + 1 < line.length() && line.charAt(i + 1) == '-') { inLineComment = true; break; }
                    if (ch == '#') { inLineComment = true; break; }
                    if (ch == '/' && i + 1 < line.length() && line.charAt(i + 1) == '*') { inBlockComment = true; i += 2; continue; }
                }
                if (ch == '\'' && !inDouble && !inBacktick) inSingle = !inSingle;
                else if (ch == '"' && !inSingle && !inBacktick) inDouble = !inDouble;
                else if (ch == '`' && !inSingle && !inDouble) inBacktick = !inBacktick;
                else if (ch == ';' && !inSingle && !inDouble && !inBacktick) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    i++;
                    continue;
                }
                cur.append(ch);
                i++;
            }
            cur.append('\n');
            // 行注释只影响本行，下一行恢复解析
            inLineComment = false;
        }
        if (!cur.toString().trim().isEmpty()) out.add(cur.toString());
        return out;
    }
}
