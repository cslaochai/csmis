package com.mis.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录口令加密公钥下发 VO（对齐 cslk HIS 的 /auth/publicKey 协议）
 */
@Data
public class PublicKeyVO implements Serializable {

    /** 公钥指纹（前端判断公钥是否轮换、要不要重拉） */
    private String keyId;

    /** 未压缩 SM2 公钥点（04 前缀 + X + Y，130 hex 字符），前端 sm-crypto doEncrypt 用 */
    private String publicKey;
}
