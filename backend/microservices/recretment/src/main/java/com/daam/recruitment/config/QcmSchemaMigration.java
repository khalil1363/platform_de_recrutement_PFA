package com.daam.recruitment.config;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class QcmSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureQcmIdColumns();
            migrateLegacyQuestions();
            dropLegacyRecruitmentIdColumn();
        } catch (Exception ex) {
            log.warn("QCM schema migration skipped/failed: {}", ex.getMessage());
        }
    }

    private void ensureQcmIdColumns() {
        if (!columnExists("qcm_questions", "qcm_id")) {
            jdbcTemplate.execute("ALTER TABLE qcm_questions ADD COLUMN qcm_id VARCHAR(255) NULL");
            log.info("Added qcm_id column to qcm_questions");
        }
        if (!columnExists("recruitments", "qcm_id")) {
            jdbcTemplate.execute("ALTER TABLE recruitments ADD COLUMN qcm_id VARCHAR(255) NULL");
            log.info("Added qcm_id column to recruitments");
        }
    }

    private void migrateLegacyQuestions() {
        if (!columnExists("qcm_questions", "recruitment_id")) {
            return;
        }

        List<String> recruitmentIds = jdbcTemplate.query(
                "SELECT DISTINCT recruitment_id FROM qcm_questions " +
                        "WHERE recruitment_id IS NOT NULL AND recruitment_id <> '' " +
                        "AND (qcm_id IS NULL OR qcm_id = '')",
                (rs, rowNum) -> rs.getString(1));

        for (String recruitmentId : recruitmentIds) {
            String qcmId = UUID.randomUUID().toString();
            String title = jdbcTemplate.query(
                    "SELECT title FROM recruitments WHERE recruitment_id = ? LIMIT 1",
                    rs -> rs.next() ? rs.getString(1) : null,
                    recruitmentId);
            if (title == null || title.isBlank()) {
                title = "QCM migré";
            } else {
                title = "QCM — " + title;
            }

            String createdBy = jdbcTemplate.query(
                    "SELECT created_by_rh_user_id FROM recruitments WHERE recruitment_id = ? LIMIT 1",
                    rs -> rs.next() ? rs.getString(1) : "migration",
                    recruitmentId);
            if (createdBy == null || createdBy.isBlank()) {
                createdBy = "migration";
            }

            jdbcTemplate.update(
                    "INSERT INTO qcms (qcm_id, title, description, created_by_rh_user_id, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, NOW(), NOW())",
                    qcmId,
                    title,
                    "QCM migré automatiquement depuis un recrutement",
                    createdBy);

            jdbcTemplate.update(
                    "UPDATE qcm_questions SET qcm_id = ? WHERE recruitment_id = ?",
                    qcmId,
                    recruitmentId);
            jdbcTemplate.update(
                    "UPDATE recruitments SET qcm_id = ? WHERE recruitment_id = ?",
                    qcmId,
                    recruitmentId);
            log.info("Migrated QCM questions for recruitment {} into qcm {}", recruitmentId, qcmId);
        }
    }

    private void dropLegacyRecruitmentIdColumn() {
        if (!columnExists("qcm_questions", "recruitment_id")) {
            return;
        }

        // Make nullable first in case drop fails on some MySQL setups
        try {
            jdbcTemplate.execute("ALTER TABLE qcm_questions MODIFY COLUMN recruitment_id VARCHAR(255) NULL");
            log.info("Made qcm_questions.recruitment_id nullable");
        } catch (Exception ex) {
            log.warn("Could not make recruitment_id nullable: {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE qcm_questions DROP COLUMN recruitment_id");
            log.info("Dropped legacy qcm_questions.recruitment_id column");
        } catch (Exception ex) {
            log.warn("Could not drop recruitment_id column (left nullable): {}", ex.getMessage());
        }
    }

    private boolean columnExists(String table, String column) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SHOW COLUMNS FROM " + table + " LIKE ?",
                column);
        return !columns.isEmpty();
    }
}
