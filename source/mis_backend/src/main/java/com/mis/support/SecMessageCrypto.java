package com.mis.support;

import cn.hutool.core.util.HexUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mis.config.CryptoProperties;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.core.io.Resource;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 医保报文安全信封（CSSSL-SEC-V1）：SM4 加密业务报文 + SM2(SM3withSM2) 签名 + 发送方证书链随报文携带。
 *
 * <p>信封结构：{@code {version, encData, sign, certSerial, timeStamp, certPem}}，canonical 待签串
 * {@code encData|timeStamp|certSerial}（与 csssl TSA 令牌规范化风格一致）。
 *
 * <p>验签侧流程：时间窗防重放 → 证书序列号与信封一致 → 叶子由 SUB_CA 签发、SUB_CA 由 csssl 根签发
 * （本地信任链，离线可验）→ 有效期 → SM3withSM2 验签 → SM4 解密还原业务报文。
 */
@Slf4j
public class SecMessageCrypto {

    public static final String VERSION = "CSSSL-SEC-V1";

    private final CryptoProperties props;
    private final PrivateKey localPrivateKey;
    private final String localCertSerial;
    private final List<X509Certificate> localChain;
    private final X509Certificate trustRoot;
    private final X509Certificate trustSubCa;
    private final byte[] sm4Key;
    private final ObjectMapper objectMapper = new ObjectMapper();

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public SecMessageCrypto(CryptoProperties props) {
        this.props = props;
        this.sm4Key = HexUtil.decodeHex(props.getSm4Key());
        this.localPrivateKey = readPrivateKey(props.getPrivateKey());
        this.localChain = readCertChain(props.getCertChain());
        this.localCertSerial = localChain.get(0).getSerialNumber().toString(16);
        this.trustRoot = readSingleCert(props.getRootCa());
        this.trustSubCa = readSingleCert(props.getSubCa());
    }

    /** 业务报文 → 安全信封 JSON */
    public String buildEnvelope(String businessJson) {
        String encData = sm4Encrypt(businessJson);
        String timeStamp = Instant.now().toString();
        String canonical = canonical(encData, timeStamp, localCertSerial);
        String signHex = HexUtil.encodeHexStr(sm2Sign(canonical));
        ObjectNode node = objectMapper.createObjectNode();
        node.put("version", VERSION);
        node.put("encData", encData);
        node.put("sign", signHex);
        node.put("certSerial", localCertSerial);
        node.put("timeStamp", timeStamp);
        node.put("certPem", Base64.getEncoder().encodeToString(chainPemBytes()));
        return node.toString();
    }

    /**
     * 安全信封 JSON → 业务报文明文。校验失败抛 IllegalStateException（携带可读原因）。
     */
    public String unpackEnvelope(String envelopeJson) {
        try {
            JsonNode env = objectMapper.readTree(envelopeJson);
            if (!VERSION.equals(env.path("version").asText())) {
                throw new IllegalStateException("不支持的报文安全版本：" + env.path("version").asText());
            }
            String encData = env.path("encData").asText();
            String signHex = env.path("sign").asText();
            String certSerial = env.path("certSerial").asText();
            String timeStamp = env.path("timeStamp").asText();
            String certPemB64 = env.path("certPem").asText();
            if (encData.isBlank() || signHex.isBlank() || certSerial.isBlank()
                    || timeStamp.isBlank() || certPemB64.isBlank()) {
                throw new IllegalStateException("安全信封字段缺失（encData/sign/certSerial/timeStamp/certPem）");
            }
            // 1. 时间窗防重放
            Instant ts = Instant.parse(timeStamp);
            long skew = Math.abs(Instant.now().getEpochSecond() - ts.getEpochSecond());
            if (skew > props.getClockSkewSeconds()) {
                throw new IllegalStateException("报文时间戳超出时间窗（" + skew + "s > " + props.getClockSkewSeconds() + "s），疑似重放");
            }
            // 2. 证书链校验（到 csssl 根）
            List<X509Certificate> chain = parseCertChain(Base64.getDecoder().decode(certPemB64));
            X509Certificate leaf = chain.get(0);
            if (!leaf.getSerialNumber().toString(16).equalsIgnoreCase(certSerial)) {
                throw new IllegalStateException("证书序列号与信封不一致");
            }
            leaf.checkValidity();
            trustSubCa.checkValidity();
            trustRoot.checkValidity();
            leaf.verify(trustSubCa.getPublicKey());
            trustSubCa.verify(trustRoot.getPublicKey());
            // 3. 验签
            String canonical = canonical(encData, timeStamp, certSerial);
            if (!sm2Verify(leaf.getPublicKey(), canonical, HexUtil.decodeHex(signHex))) {
                throw new IllegalStateException("SM2 验签失败：报文被篡改或签名值不符");
            }
            // 4. 解密还原业务报文
            return sm4Decrypt(encData);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("报文安全信封解析失败：" + e.getMessage(), e);
        }
    }

    private String canonical(String encData, String timeStamp, String certSerial) {
        return encData + "|" + timeStamp + "|" + certSerial;
    }

    private String sm4Encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sm4Key, "SM4"));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SM4 加密失败：" + e.getMessage(), e);
        }
    }

    private String sm4Decrypt(String b64) {
        try {
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sm4Key, "SM4"));
            return new String(cipher.doFinal(Base64.getDecoder().decode(b64)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("SM4 解密失败（会话密钥不一致或密文被破坏）", e);
        }
    }

    private byte[] sm2Sign(String content) {
        try {
            Signature sig = Signature.getInstance("SM3withSM2", "BC");
            sig.initSign(localPrivateKey);
            sig.update(content.getBytes(StandardCharsets.UTF_8));
            return sig.sign();
        } catch (Exception e) {
            throw new IllegalStateException("SM2 签名失败：" + e.getMessage(), e);
        }
    }

    private boolean sm2Verify(PublicKey publicKey, String content, byte[] signDer) {
        try {
            // 证书公钥 SPKI 的 OID 1.2.840.10045.3.1.7 会被 BC 默认解析为 NIST P-256；
            // 必须取出点坐标后按 GM 曲线（sm2p256v1）构造公钥参数再验签，否则曲线不同必然失败
            java.security.interfaces.ECPublicKey ecPub = (java.security.interfaces.ECPublicKey) publicKey;
            java.security.spec.ECPoint w = ecPub.getW();
            org.bouncycastle.jce.spec.ECParameterSpec gmSpec =
                    org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("sm2p256v1");
            org.bouncycastle.math.ec.ECPoint q = gmSpec.getCurve().createPoint(w.getAffineX(), w.getAffineY());
            org.bouncycastle.crypto.params.ECDomainParameters domain =
                    new org.bouncycastle.crypto.params.ECDomainParameters(gmSpec.getCurve(), gmSpec.getG(), gmSpec.getN(), gmSpec.getH());
            org.bouncycastle.crypto.params.ECPublicKeyParameters pubParams =
                    new org.bouncycastle.crypto.params.ECPublicKeyParameters(q, domain);
            org.bouncycastle.crypto.Signer signer = new org.bouncycastle.crypto.signers.SM2Signer();
            signer.init(false, pubParams);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            signer.update(data, 0, data.length);
            return signer.verifySignature(signDer);
        } catch (Exception e) {
            log.warn("[报文安全] SM2 验签异常：{}", e.getMessage());
            return false;
        }
    }

    private byte[] chainPemBytes() {
        StringBuilder sb = new StringBuilder();
        for (X509Certificate cert : localChain) {
            sb.append(PemUtil.certToPem(cert));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private PrivateKey readPrivateKey(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return PemUtil.readPrivateKey(pem);
        } catch (Exception e) {
            throw new IllegalStateException("SM2 私钥加载失败：" + resource.getDescription(), e);
        }
    }

    private List<X509Certificate> readCertChain(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return parseCertChain(in.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("证书链加载失败：" + resource.getDescription(), e);
        }
    }

    private X509Certificate readSingleCert(Resource resource) {
        List<X509Certificate> list = readCertChain(resource);
        if (list.isEmpty()) {
            throw new IllegalStateException("信任证书为空：" + resource.getDescription());
        }
        return list.get(0);
    }

    private List<X509Certificate> parseCertChain(byte[] pemBytes) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
        List<X509Certificate> out = new ArrayList<>();
        for (java.security.cert.Certificate c : cf.generateCertificates(new java.io.ByteArrayInputStream(pemBytes))) {
            out.add((X509Certificate) c);
        }
        if (out.isEmpty()) {
            throw new IllegalStateException("PEM 中无证书");
        }
        return out;
    }
}
