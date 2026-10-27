package com.mis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 医保局端（csmis）报文安全配置（jinb.crypto.*）。
 *
 * <p>对齐 XJ-D01-2019 医保接口国密要求：前置机→医保局 方向报文
 * SM4 加密 + SM2 签名 + 发送方证书链（csssl 签发）验签。
 * 信任根为 csssl 根证书（root-ca.pem），本地离线校验链，不依赖前置在线。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jinb.crypto")
public class CryptoProperties {

    /** 国密开关：false=明文透传（开发期）；true=验签解密 + 加密签名回执 */
    private boolean enabled = false;

    /** 本端→对端 SM4 会话密钥（32 位十六进制，与前置机 mss 方向一致；生产按会话协商） */
    private String sm4Key = "";

    /** 本端 SM2 签名私钥 PEM（classpath 资源路径） */
    private Resource privateKey;

    /** 本端证书链 PEM（叶子 + SUB_CA，csssl 签发） */
    private Resource certChain;

    /** 信任根：csssl 根证书 PEM */
    private Resource rootCa;

    /** 信任链：csssl SUB_CA 证书 PEM（签发叶子证书的二级 CA） */
    private Resource subCa;

    /** 报文时间窗（秒），超窗按重放拒绝，默认 5 分钟 */
    private int clockSkewSeconds = 300;
}
