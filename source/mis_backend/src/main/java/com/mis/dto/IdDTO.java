package com.mis.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用主键入参（主键一律 Long，前端字符串 id 由 Jackson 自动反序列化）
 */
@Data
public class IdDTO implements Serializable {

    private Long id;
}
