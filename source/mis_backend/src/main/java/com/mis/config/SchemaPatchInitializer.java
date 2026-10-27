package com.mis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 存量库结构补丁（启动时执行，幂等）：schema.sql 全部 IF NOT EXISTS，只对新建库生效；
 * 已存在的表加列走这里，列已存在（Duplicate column）静默跳过。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(1)
public class SchemaPatchInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        patch("mss_settlement 加 2301 个账实扣列（撤销冲正退回个账用）",
                "ALTER TABLE mss_settlement ADD COLUMN account_deduct DECIMAL(12,2) DEFAULT 0 "
                        + "COMMENT '2301个账实扣（撤销冲正退回个账）'");
    }

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
}
