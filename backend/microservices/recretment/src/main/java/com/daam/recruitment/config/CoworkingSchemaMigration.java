package com.daam.recruitment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures coworking columns exist on recruitments (safe if ddl-auto already added them).
 */
@Slf4j
@Component
@Order(26)
@RequiredArgsConstructor
public class CoworkingSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            addColumnIfMissing(
                    "recruitments",
                    "coworking",
                    "ALTER TABLE recruitments ADD COLUMN coworking BIT(1) NOT NULL DEFAULT 0");
            addColumnIfMissing(
                    "recruitments",
                    "coworking_month",
                    "ALTER TABLE recruitments ADD COLUMN coworking_month DATE NULL");
        } catch (Exception ex) {
            log.warn("Coworking schema migration skipped/failed: {}", ex.getMessage());
        }
    }

    private void addColumnIfMissing(String table, String column, String alterSql) {
        Integer count = jdbcTemplate.query(
                """
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                rs -> rs.next() ? rs.getInt(1) : 0,
                table,
                column);
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            log.info("Added column {}.{}", table, column);
        }
    }
}
