package com.smartcampus.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Drops stale Postgres CHECK constraints on enum columns so new enum values
// don't trip them — ddl-auto=update won't re-sync these once created.
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class NotificationSchemaFix implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        dropConstraintIfExists("notifications", "notifications_type_check");
        dropOldPreferenceTableIfShapeChanged();
    }

    private void dropConstraintIfExists(String table, String constraint) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
            log.debug("Ensured CHECK constraint {} on {} is removed", constraint, table);
        } catch (Exception e) {
            log.warn("Could not drop {} on {}: {}", constraint, table, e.getMessage());
        }
    }

    private void dropOldPreferenceTableIfShapeChanged() {
        try {
            Integer hasTypeColumn = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                            "WHERE table_name = 'user_notification_preferences' AND column_name = 'type'",
                    Integer.class);
            if (hasTypeColumn != null && hasTypeColumn > 0) {
                jdbcTemplate.execute("DROP TABLE IF EXISTS user_notification_preferences CASCADE");
                log.info("Dropped legacy user_notification_preferences table for schema migration");
            }
        } catch (Exception e) {
            log.warn("Could not check/drop old preference table: {}", e.getMessage());
        }
    }
}
