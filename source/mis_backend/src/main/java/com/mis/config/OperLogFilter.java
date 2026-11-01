package com.mis.config;

import com.mis.service.OperLogService;
import com.mis.vo.OperLogVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志拦截器（审计留痕）。
 * <p>医保是强审计行业，所有「写」操作必须留痕：谁、在何时、从哪个 IP、对哪张单、做了什么、结果如何。
 * <p>实现要点：
 * <ul>
 *   <li>只记录写操作（POST/PUT/DELETE）与敏感读操作（登录），避免查询把日志表撑爆；</li>
 *   <li>请求体在 Servlet 层读一次需包装，否则 Controller 拿不到 body —— 用 ContentCachingRequestWrapper；</li>
 *   <li>写日志走异步线程池，失败只告警不阻断主流程；</li>
 *   <li>敏感字段（密码、证件号）在落库前脱敏。</li>
 * </ul>
 */
@Slf4j
@Component
// 必须在 AuthFilter 之后（order 更大 = 过滤器链更内层）：
// 这样 finally 里读 UserContext 时，AuthFilter 尚未执行 clear()，能拿到操作人。
// AuthFilter 未标 @Order，默认 LOWEST_PRECEDENCE（最大），本身就在最内层，
// 故这里取一个较大但小于 LOWEST_PRECEDENCE 的值，确保顺序稳定。
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RequiredArgsConstructor
public class OperLogFilter extends OncePerRequestFilter {

    private final OperLogService operLogService;

    /** 不记录的路径前缀 */
    private static final Set<String> SKIP_PREFIX = Set.of(
            "/auth/login", "/auth/publicKey", "/gateway/", "/doc.html",
            "/swagger-ui", "/v3/api-docs", "/webjars", "/favicon.ico", "/error");

    /** 需要脱敏的请求参数名 */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "oldPassword", "newPassword", "idCard", "id_card", "bankAccount", "cardNo");

    /** 敏感读操作也记录 */
    private static final Set<String> RECORD_READ = Set.of("/auth/info", "/auth/menus");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathOf(request);
        for (String p : SKIP_PREFIX) {
            if (path.equals(p) || (p.endsWith("/") && path.startsWith(p))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        boolean isWrite = !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)
                && !"OPTIONS".equalsIgnoreCase(method);
        boolean isSensitiveRead = RECORD_READ.contains(pathOf(request));
        if (!isWrite && !isSensitiveRead) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            try {
                record(wrapped, response, method, pathOf(request), System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.debug("[操作日志] 记录异常（已忽略）：{}", ex.getMessage());
            }
        }
    }

    private void record(ContentCachingRequestWrapper request, HttpServletResponse response,
                        String method, String uri, long cost) {
        OperLogVO vo = new OperLogVO();
        vo.setRequestUri(uri);
        vo.setRequestMethod(method);
        vo.setCostMs(cost);
        vo.setOperIp(clientIp(request));
        vo.setOperModule(moduleOf(uri));
        vo.setOperType(operTypeOf(method, uri));

        LoginUser user = UserContext.get();
        if (user != null) {
            vo.setOperUser(user.getRealName() != null && !user.getRealName().isBlank()
                    ? user.getRealName() : user.getUsername());
            vo.setOperUserId(user.getUserId());
        }

        int status = response.getStatus();
        vo.setOperResult(status >= 200 && status < 400 ? 1 : 0);
        if (status >= 400) {
            vo.setErrorMsg("HTTP " + status);
        }

        vo.setRequestParams(buildParams(request));

        // 目标单据：优先从 body 里捞 id / no 这类字段
        extractTarget(request, vo);

        operLogService.save(vo);
    }

    /** 从请求体中提取业务对象线索（id / 单号） */
    private void extractTarget(ContentCachingRequestWrapper request, OperLogVO vo) {
        String body = bodyOf(request);
        if (!StringUtils.hasText(body) || body.length() > 4000) {
            return;
        }
        vo.setTargetType(targetTypeOf(vo.getRequestUri()));
        String id = matchJson(body, "\"id\"");
        String no = matchJson(body, "\"billNo\"", "\"bizNo\"", "\"clueNo\"", "\"recordNo\"",
                "\"suspicionNo\"", "\"taskNo\"", "\"no\"");
        if (StringUtils.hasText(id)) {
            try {
                vo.setTargetId(Long.parseLong(id.replaceAll("[^0-9]", "")));
            } catch (NumberFormatException ignore) {
                // 非数字 ID，跳过
            }
        }
        if (StringUtils.hasText(no)) {
            vo.setTargetNo(no);
        }
    }

    /** 拼接请求参数（body 优先，其次 query），敏感字段脱敏 */
    private String buildParams(ContentCachingRequestWrapper request) {
        StringBuilder sb = new StringBuilder();
        try {
            String body = bodyOf(request);
            if (StringUtils.hasText(body)) {
                sb.append(mask(body));
            }
            Map<String, String[]> params = request.getParameterMap();
            if (params != null && !params.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                List<String> parts = new ArrayList<>();
                for (Map.Entry<String, String[]> e : params.entrySet()) {
                    String key = e.getKey();
                    String val = e.getValue() != null && e.getValue().length > 0 ? e.getValue()[0] : "";
                    parts.add(key + "=" + (SENSITIVE_KEYS.contains(key) ? maskValue(val) : val));
                }
                sb.append(String.join("&", parts));
            }
        } catch (Exception ignore) {
            // 参数拼装失败不影响主流程
        }
        String s = sb.toString();
        return s.length() > 2000 ? s.substring(0, 2000) + "..." : s;
    }

    /** body 中的敏感键值脱敏 */
    private String mask(String body) {
        String out = body;
        for (String k : SENSITIVE_KEYS) {
            out = out.replaceAll("(\"" + k + "\"\\s*:\\s*\")[^\"]*(\")", "$1******$2");
        }
        return out;
    }

    private String maskValue(String v) {
        if (v == null || v.length() <= 4) {
            return "******";
        }
        return v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }

    private String matchJson(String body, String... keys) {
        for (String k : keys) {
            int i = body.indexOf(k);
            if (i < 0) {
                continue;
            }
            int colon = body.indexOf(':', i + k.length());
            if (colon < 0) {
                continue;
            }
            int q1 = body.indexOf('"', colon + 1);
            if (q1 < 0) {
                continue;
            }
            int q2 = body.indexOf('"', q1 + 1);
            if (q2 < 0) {
                continue;
            }
            return body.substring(q1 + 1, q2);
        }
        return null;
    }

    /** 操作类型：按 HTTP 方法 + 路径语义判定 */
    private String operTypeOf(String method, String uri) {
        String u = uri.toLowerCase();
        if (u.contains("/login") || u.contains("/logout")) {
            return "登录";
        }
        if ("GET".equalsIgnoreCase(method)) {
            return "查询";
        }
        if (u.contains("/export")) {
            return "导出";
        }
        if (u.contains("/audit") || u.contains("/review") || u.contains("/handle")
                || u.contains("/confirm") || u.contains("/confer") || u.contains("/approve")) {
            return "审核";
        }
        if (u.contains("/delete") || u.contains("/remove")) {
            return "删除";
        }
        if (u.contains("/upsert") || u.contains("/save") || u.contains("/add") || u.contains("/create")) {
            return "新增";
        }
        if (u.contains("/update") || u.contains("/edit") || u.contains("/change") || u.contains("/status")) {
            return "修改";
        }
        return "其他";
    }

    /** 功能模块：取路径第一段 */
    private String moduleOf(String uri) {
        String p = uri.startsWith("/") ? uri.substring(1) : uri;
        int i = p.indexOf('/');
        String m = i > 0 ? p.substring(0, i) : p;
        return switch (m) {
            case "supervise" -> "基金监管";
            case "settle" -> "结算管理";
            case "insure" -> "参保管理";
            case "audit" -> "费用审核";
            case "agreement" -> "定点协议";
            case "claim" -> "手工报销";
            case "stats" -> "统计报表";
            case "stock" -> "进销存";
            case "drg" -> "DRG监管";
            case "auth" -> "系统管理";
            case "system" -> "系统管理";
            case "settlebill" -> "结算单";
            default -> m;
        };
    }

    /** 业务对象类型：取路径第二段 */
    private String targetTypeOf(String uri) {
        String p = uri.startsWith("/") ? uri.substring(1) : uri;
        String[] seg = p.split("/");
        if (seg.length >= 2) {
            return seg[1];
        }
        return null;
    }

    private String pathOf(HttpServletRequest request) {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        return path;
    }

    private String bodyOf(ContentCachingRequestWrapper request) {
        try {
            byte[] b = request.getContentAsByteArray();
            if (b == null || b.length == 0) {
                return null;
            }
            return new String(b, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (StringUtils.hasText(v) && !"unknown".equalsIgnoreCase(v)) {
                int i = v.indexOf(',');
                return i > 0 ? v.substring(0, i).trim() : v.trim();
            }
        }
        String ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
