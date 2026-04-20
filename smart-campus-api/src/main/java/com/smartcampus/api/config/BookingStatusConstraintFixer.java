package com.smartcampus.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate's ddl-auto=update does not rewrite enum CHECK constraints when new values
 * are added to the enum. Databases created before new enum values are introduced
 * still have a check that only permits the old set, which makes writes with new
 * values fail with ConstraintViolationException.
 *
 * This runner rebuilds the relevant constraints idempotently at startup so any
 * database that upgraded from an older schema keeps working without a manual SQL
 * step.
 */
@Slf4j
@Component
@Profile("!test")
@Order(0)
@RequiredArgsConstructor
public class BookingStatusConstraintFixer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        rebuild("bookings", "status", "bookings_status_check",
                "PENDING", "APPROVED", "REJECTED", "CANCELLED", "COMPLETED");
        rebuild("notifications", "type", "notifications_type_check",
                "ENROLLMENT_CONFIRMED", "ENROLLMENT_WAITLISTED", "WAITLIST_PROMOTED",
                "ENROLLMENT_WITHDRAWN", "GRADE_RELEASED", "GRADE_UPDATED", "COURSE_STATUS_CHANGED",
                "BOOKING_CREATED", "BOOKING_APPROVED", "BOOKING_REJECTED", "BOOKING_CANCELLED",
                "BOOKING_COMPLETED",
                "TICKET_CREATED", "TICKET_ASSIGNED", "TICKET_UPDATED", "TICKET_RESOLVED",
                "SYSTEM_ANNOUNCEMENT", "ANNOUNCEMENT", "GENERAL");
        rebuild("pending_notification_deliveries", "type",
                "pending_notification_deliveries_type_check",
                "ENROLLMENT_CONFIRMED", "ENROLLMENT_WAITLISTED", "WAITLIST_PROMOTED",
                "ENROLLMENT_WITHDRAWN", "GRADE_RELEASED", "GRADE_UPDATED", "COURSE_STATUS_CHANGED",
                "BOOKING_CREATED", "BOOKING_APPROVED", "BOOKING_REJECTED", "BOOKING_CANCELLED",
                "BOOKING_COMPLETED",
                "TICKET_CREATED", "TICKET_ASSIGNED", "TICKET_UPDATED", "TICKET_RESOLVED",
                "SYSTEM_ANNOUNCEMENT", "ANNOUNCEMENT", "GENERAL");
    }

    private void rebuild(String table, String column, String constraint, String... allowedValues) {
        try {
            Integer hasTable = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?",
                    Integer.class, table);
            if (hasTable == null || hasTable == 0) {
                return;
            }
            String inList = String.join(",",
                    java.util.Arrays.stream(allowedValues).map(v -> "'" + v + "'").toList());
            jdbc.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
            jdbc.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + constraint
                    + " CHECK (" + column + " IN (" + inList + "))");
            log.info("Rebuilt {} on {}.{}.", constraint, table, column);
        } catch (Exception e) {
            log.warn("Could not rebuild {}: {}", constraint, e.getMessage());
        }
    }
}
