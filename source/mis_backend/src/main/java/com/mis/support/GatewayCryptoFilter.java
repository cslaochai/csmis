package com.mis.support;

import com.mis.config.CryptoProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 医保网关报文安全过滤器：对 {@code /gateway/*} 与 {@code /insure/person/query} 做
 * 「验签 + 解密（入）→ 业务处理 → 签名 + 加密（出）」。
 *
 * <p>开启条件：{@code jinb.crypto.enabled=true}；关闭时原样透传（controller 零改动）。
 * 校验失败返回统一错误结构（code=500 + 原因），与前置机/HIS 解析口径一致，不抛 HTTP 5xx。
 */
@Slf4j
@Component
@Order(10)
public class GatewayCryptoFilter extends OncePerRequestFilter {

    private final CryptoProperties cryptoProps;
    private volatile SecMessageCrypto crypto;

    public GatewayCryptoFilter(CryptoProperties cryptoProps) {
        this.cryptoProps = cryptoProps;
    }

    private SecMessageCrypto crypto() {
        if (crypto == null) {
            synchronized (this) {
                if (crypto == null) {
                    crypto = new SecMessageCrypto(cryptoProps);
                    log.info("[报文安全] 医保网关国密通道就绪（验签+SM4解密），时间窗 {}s", cryptoProps.getClockSkewSeconds());
                }
            }
        }
        return crypto;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!cryptoProps.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.matches(".*/gateway/[^/]+/?$")
                && !path.matches(".*/insure/person/query/?$");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String body = readBody(request);
        if (body == null || body.isBlank()) {
            log.warn("[报文安全] 医保网关请求体为空 uri={}", request.getRequestURI());
            writeError(response, "医保网关请求体为空");
            return;
        }
        String business;
        try {
            business = crypto().unpackEnvelope(body);
            log.info("[报文安全] 解包成功 uri={} body前80={}", request.getRequestURI(),
                    business.length() > 80 ? business.substring(0, 80) : business);
        } catch (Exception e) {
            log.warn("[报文安全] 验签解密失败 uri={} body前80={} err={}", request.getRequestURI(),
                    body.length() > 80 ? body.substring(0, 80) : body, e.getMessage());
            writeError(response, "报文安全校验失败：" + e.getMessage());
            return;
        }
        // 业务层读到的是解密的明文体（@RequestBody String/DTO 从包装流读取）
        DecryptedRequest wrapped = new DecryptedRequest(request, business);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrapped, responseWrapper);
        } finally {
            byte[] bizBytes = responseWrapper.getContentAsByteArray();
            if (bizBytes.length == 0) {
                // 无内容（如 302/异常页），原样提交避免二次包装
                responseWrapper.copyBodyToResponse();
                return;
            }
            String biz = new String(bizBytes, StandardCharsets.UTF_8);
            try {
                String envelope = crypto().buildEnvelope(biz);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getOutputStream().write(envelope.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("[报文安全] 回执加密签名失败：{}", e.getMessage(), e);
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void writeError(HttpServletResponse response, String msg) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(("{\"code\":500,\"message\":\"" + msg.replace("\"", "'") + "\",\"data\":null}")
                .getBytes(StandardCharsets.UTF_8));
    }

    /** 用解密后的业务报文替换请求体 */
    static class DecryptedRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        DecryptedRequest(HttpServletRequest request, String businessBody) {
            super(request);
            this.body = businessBody.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // 同步读取，无需监听器
                }

                @Override
                public int read() {
                    return in.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
