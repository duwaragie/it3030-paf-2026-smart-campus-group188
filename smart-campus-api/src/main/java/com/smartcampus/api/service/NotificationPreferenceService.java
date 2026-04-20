package com.smartcampus.api.service;

import com.smartcampus.api.dto.NotificationPreferenceDTO;
import com.smartcampus.api.exception.ResourceNotFoundException;
import com.smartcampus.api.model.PendingNotificationDelivery;
import com.smartcampus.api.model.User;
import com.smartcampus.api.model.UserNotificationPreference;
import com.smartcampus.api.repository.PendingNotificationDeliveryRepository;
import com.smartcampus.api.repository.UserNotificationPreferenceRepository;
import com.smartcampus.api.repository.UserRepository;
import com.smartcampus.api.service.notification.QuietHours;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Defaults (email ON, push OFF) are applied in-memory so the DB only stores
// rows for users who've deviated.
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private static final boolean DEFAULT_EMAIL = true;
    private static final boolean DEFAULT_PUSH = false;
    private static final boolean DEFAULT_QUIET_HOURS_ENABLED = false;
    private static final LocalTime DEFAULT_QUIET_START = LocalTime.of(22, 0);
    private static final LocalTime DEFAULT_QUIET_END = LocalTime.of(7, 0);

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final PendingNotificationDeliveryRepository pendingRepository;

    @Transactional(readOnly = true)
    public NotificationPreferenceDTO getForUser(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .map(this::toDTO)
                .orElseGet(() -> NotificationPreferenceDTO.builder()
                        .email(DEFAULT_EMAIL)
                        .push(DEFAULT_PUSH)
                        .quietHoursEnabled(DEFAULT_QUIET_HOURS_ENABLED)
                        .quietHoursStart(DEFAULT_QUIET_START)
                        .quietHoursEnd(DEFAULT_QUIET_END)
                        .build());
    }

    @Transactional
    public NotificationPreferenceDTO update(Long userId, NotificationPreferenceDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserNotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserNotificationPreference.builder()
                        .user(user)
                        .email(DEFAULT_EMAIL)
                        .push(DEFAULT_PUSH)
                        .quietHoursEnabled(DEFAULT_QUIET_HOURS_ENABLED)
                        .quietHoursStart(DEFAULT_QUIET_START)
                        .quietHoursEnd(DEFAULT_QUIET_END)
                        .build());

        if (dto.getEmail() != null) pref.setEmail(dto.getEmail());
        if (dto.getPush() != null) pref.setPush(dto.getPush());
        if (dto.getQuietHoursEnabled() != null) pref.setQuietHoursEnabled(dto.getQuietHoursEnabled());
        if (dto.getQuietHoursStart() != null) pref.setQuietHoursStart(dto.getQuietHoursStart());
        if (dto.getQuietHoursEnd() != null) pref.setQuietHoursEnd(dto.getQuietHoursEnd());

        pref = preferenceRepository.save(pref);
        resyncPendingDeliveries(userId, pref);
        return toDTO(pref);
    }

    // When a user changes their quiet-hours window (or turns it off), re-snap
    // any already-queued email/push rows to the new schedule. If they're no
    // longer in quiet hours per the new setting, deliverAt goes to `now` so the
    // flusher picks them up on the next minute tick.
    private void resyncPendingDeliveries(Long userId, UserNotificationPreference pref) {
        List<PendingNotificationDelivery> pending = pendingRepository.findByRecipientId(userId);
        if (pending.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newResumeAt = QuietHours.resumeAtOrNull(pref, now);
        LocalDateTime target = newResumeAt != null ? newResumeAt : now;
        for (PendingNotificationDelivery p : pending) {
            p.setDeliverAt(target);
        }
        pendingRepository.saveAll(pending);
    }

    private NotificationPreferenceDTO toDTO(UserNotificationPreference pref) {
        return NotificationPreferenceDTO.builder()
                .email(pref.getEmail())
                .push(pref.getPush())
                .quietHoursEnabled(pref.getQuietHoursEnabled() != null
                        ? pref.getQuietHoursEnabled() : DEFAULT_QUIET_HOURS_ENABLED)
                .quietHoursStart(pref.getQuietHoursStart() != null
                        ? pref.getQuietHoursStart() : DEFAULT_QUIET_START)
                .quietHoursEnd(pref.getQuietHoursEnd() != null
                        ? pref.getQuietHoursEnd() : DEFAULT_QUIET_END)
                .build();
    }
}
