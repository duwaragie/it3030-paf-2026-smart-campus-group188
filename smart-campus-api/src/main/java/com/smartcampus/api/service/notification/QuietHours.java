package com.smartcampus.api.service.notification;

import com.smartcampus.api.model.UserNotificationPreference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Shared quiet-hours math. Used by the dispatcher at queue time and by the
// preference service when a user changes their window — both need the same
// "is this user in quiet hours, and if so when does it end?" logic.
public final class QuietHours {

    private QuietHours() {}

    // Returns when the user's quiet window ends, or null if they're not in one
    // right now (so sends should go out immediately).
    public static LocalDateTime resumeAtOrNull(UserNotificationPreference pref, LocalDateTime now) {
        if (pref == null || !Boolean.TRUE.equals(pref.getQuietHoursEnabled())) return null;
        LocalTime start = pref.getQuietHoursStart();
        LocalTime end = pref.getQuietHoursEnd();
        if (start == null || end == null || start.equals(end)) return null;

        LocalTime nowTime = now.toLocalTime();
        boolean inside = start.isBefore(end)
                ? !nowTime.isBefore(start) && nowTime.isBefore(end)
                : !nowTime.isBefore(start) || nowTime.isBefore(end);
        if (!inside) return null;

        // Resume at the next occurrence of `end`. Same-day windows resolve to
        // today@end; overnight windows go tomorrow@end while we're in the
        // evening portion and today@end once we've passed midnight.
        LocalDate endDate = (start.isBefore(end) || nowTime.isBefore(end))
                ? now.toLocalDate()
                : now.toLocalDate().plusDays(1);
        return LocalDateTime.of(endDate, end);
    }
}
