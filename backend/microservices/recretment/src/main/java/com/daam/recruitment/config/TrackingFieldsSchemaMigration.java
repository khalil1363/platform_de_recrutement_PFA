package com.daam.recruitment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures fiche-suivi Excel columns exist on job_applications.
 */
@Slf4j
@Component
@Order(27)
@RequiredArgsConstructor
public class TrackingFieldsSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            addColumnIfMissing(
                    "job_applications",
                    "hebergement",
                    "ALTER TABLE job_applications ADD COLUMN hebergement VARCHAR(255) NULL");
            addColumnIfMissing(
                    "job_applications",
                    "date_debut_potentielle",
                    "ALTER TABLE job_applications ADD COLUMN date_debut_potentielle DATE NULL");
            addColumnIfMissing(
                    "job_applications",
                    "entretien_resp_at",
                    "ALTER TABLE job_applications ADD COLUMN entretien_resp_at DATETIME NULL");
        } catch (Exception ex) {
            log.warn("Tracking fields schema migration skipped/failed: {}", ex.getMessage());
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
