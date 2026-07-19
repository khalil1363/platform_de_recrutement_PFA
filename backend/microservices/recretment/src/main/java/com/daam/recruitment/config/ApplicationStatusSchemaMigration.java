package com.daam.recruitment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * job_applications.status was created as a MySQL ENUM without HIRED.
 * Convert to VARCHAR so ApplicationStatus can grow safely.
 */
@Slf4j
@Component
@Order(25)
@RequiredArgsConstructor
public class ApplicationStatusSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String columnType = jdbcTemplate.query(
                    """
                    SELECT DATA_TYPE FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'job_applications'
                      AND COLUMN_NAME = 'status'
                    LIMIT 1
                    """,
                    rs -> rs.next() ? rs.getString(1) : null);

            if (columnType != null && "enum".equalsIgnoreCase(columnType)) {
                jdbcTemplate.execute(
                        "ALTER TABLE job_applications MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED'");
                log.info("Migrated job_applications.status from ENUM to VARCHAR(20)");
            }
        } catch (Exception ex) {
            log.warn("Application status schema migration skipped/failed: {}", ex.getMessage());
        }
    }
}
