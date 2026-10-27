package com.mis.support;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PEM 工具（轻量）：私钥解析（显式 GM 曲线，规避 sm2p256v1 与 prime256v1 共用 OID 时 BC 按 NIST 解析的坑）、
 * X.509 证书转 PEM。
 */
public final class PemUtil {

    private static final Pattern PRIVATE_KEY = Pattern.compile(
            "-----BEGIN (EC )?PRIVATE KEY-----\\s*([A-Za-z0-9+/=\\r\\n]+)\\s*-----END (EC )?PRIVATE KEY-----");
    private static final Pattern CERT = Pattern.compile(
            "-----BEGIN CERTIFICATE-----\\s*([A-Za-z0-9+/=\\r\\n]+)\\s*-----END CERTIFICATE-----");

    private PemUtil() {
    }

    /**
     * 从 PEM 解析 SM2 私钥：提取 d 后按 GM 曲线（sm2p256v1）显式构造。
     * 兼容 PKCS#8（BEGIN PRIVATE KEY）与 SEC1（BEGIN EC PRIVATE KEY）两种头。
     * 必须显式指定 GM 曲线——OID 1.2.840.10045.3.1.7 在 BC 默认映射为 NIST P-256，
     * 若走 OID 解析会把同一标量 d 误判到错误曲线上，签名无法被 GM 公钥验签通过。
     */
    public static PrivateKey readPrivateKey(String pem) {
        Matcher m = PRIVATE_KEY.matcher(pem);
        if (!m.find()) {
            throw new IllegalStateException("PEM 中未找到私钥块（需 BEGIN PRIVATE KEY 或 BEGIN EC PRIVATE KEY）");
        }
        try {
            byte[] der = Base64.getDecoder().decode(m.group(2).replaceAll("\\s", ""));
            BigInteger d;
            if (m.group(1) != null) {
                d = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(der).getKey();
            } else {
                org.bouncycastle.asn1.pkcs.PrivateKeyInfo pki =
                        org.bouncycastle.asn1.pkcs.PrivateKeyInfo.getInstance(der);
                d = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(pki.parsePrivateKey()).getKey();
            }
            org.bouncycastle.jce.spec.ECParameterSpec gmSpec =
                    org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("sm2p256v1");
            org.bouncycastle.jce.spec.ECPrivateKeySpec bcSpec =
                    new org.bouncycastle.jce.spec.ECPrivateKeySpec(d, gmSpec);
            return KeyFactory.getInstance("EC", "BC").generatePrivate(bcSpec);
        } catch (Exception e) {
            throw new IllegalStateException("SM2 私钥解析失败（需 GM sm2p256v1 私钥）：" + e.getMessage(), e);
        }
    }

    /** 证书转 PEM 文本 */
    public static String certToPem(X509Certificate cert) {
        try {
            String b64 = Base64.getEncoder().encodeToString(cert.getEncoded());
            StringBuilder sb = new StringBuilder("-----BEGIN CERTIFICATE-----\n");
            for (int i = 0; i < b64.length(); i += 64) {
                sb.append(b64, i, Math.min(i + 64, b64.length())).append('\n');
            }
            sb.append("-----END CERTIFICATE-----\n");
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("证书转 PEM 失败：" + e.getMessage(), e);
        }
    }

    public static byte[] pemToBytes(String pem) {
        return pem.getBytes(StandardCharsets.UTF_8);
    }
}
