package com.daam.recruitment.config;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RhZoneAssignmentSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList("SHOW INDEX FROM rh_zone_assignments");

        for (Map<String, Object> index : indexes) {
            String keyName = String.valueOf(index.get("Key_name"));
            String columnName = String.valueOf(index.get("Column_name"));
            Number nonUnique = (Number) index.get("Non_unique");

            boolean isLegacySingleRhUniqueIndex = !"PRIMARY".equalsIgnoreCase(keyName)
                    && nonUnique != null
                    && nonUnique.intValue() == 0
                    && "rh_user_id".equalsIgnoreCase(columnName);

            if (isLegacySingleRhUniqueIndex) {
                try {
                    jdbcTemplate.execute("ALTER TABLE rh_zone_assignments DROP INDEX " + keyName);
                    log.info("Dropped legacy unique index {} from rh_zone_assignments", keyName);
                } catch (Exception ex) {
                    log.warn("Could not drop legacy index {}: {}", keyName, ex.getMessage());
                }
            }
        }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE rh_zone_assignments " +
                    "ADD CONSTRAINT uk_rh_zone_assignments_rh_zone UNIQUE (rh_user_id, zone_id)"
            );
            log.info("Ensured composite unique constraint on rh_zone_assignments(rh_user_id, zone_id)");
        } catch (Exception ex) {
            log.debug("Composite RH-zone unique constraint already exists or could not be created: {}", ex.getMessage());
        }
    }
}
