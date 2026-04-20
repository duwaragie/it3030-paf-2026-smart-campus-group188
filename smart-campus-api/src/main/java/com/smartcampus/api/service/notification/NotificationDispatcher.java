package com.smartcampus.api.service.notification;

import com.smartcampus.api.model.NotificationChannelType;
import com.smartcampus.api.model.PendingNotificationDelivery;
import com.smartcampus.api.model.UserNotificationPreference;
import com.smartcampus.api.repository.PendingNotificationDeliveryRepository;
import com.smartcampus.api.repository.UserNotificationPreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// In-app always fires (can't be disabled — safety net, always persisted so the
// bell stays accurate even during quiet hours). Email and push are gated on the
// user's preference row, and during quiet hours they're queued as
// PendingNotificationDelivery rows to be flushed by QuietHoursFlushService
// when the window ends. All send paths (event-driven, scheduled announcements,
// bulk fan-out) funnel through dispatch(), so the rule applies uniformly.
@Slf4j
@Component
public class NotificationDispatcher {

    private static final boolean DEFAULT_EMAIL = true;
    private static final boolean DEFAULT_PUSH = false;

    private final Map<NotificationChannelType, NotificationChannel> channels =
            new EnumMap<>(NotificationChannelType.class);

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final PendingNotificationDeliveryRepository pendingRepository;

    public NotificationDispatcher(List<NotificationChannel> channelBeans,
                                  UserNotificationPreferenceRepository preferenceRepository,
                                  PendingNotificationDeliveryRepository pendingRepository) {
        for (NotificationChannel c : channelBeans) channels.put(c.type(), c);
        this.preferenceRepository = preferenceRepository;
        this.pendingRepository = pendingRepository;
    }

    public void dispatch(NotificationRequest request) {
        Long userId = request.getRecipient().getId();
        // Force-initialize the User proxy's string fields inside the caller's
        // transaction. The @Async EmailNotificationChannel runs on a separate
        // thread where the Hibernate session is gone, so accessing a lazy
        // proxy there throws "Illegal pop()" / LazyInitializationException.
        // Touching getEmail()/getName() here populates the proxy's cached
        // target, which the async thread can then read without a session.
        request.getRecipient().getEmail();
        request.getRecipient().getName();
        send(NotificationChannelType.IN_APP, request);

        UserNotificationPreference pref = preferenceRepository.findByUserId(userId).orElse(null);
        boolean emailOn = pref != null ? Boolean.TRUE.equals(pref.getEmail()) : DEFAULT_EMAIL;
        boolean pushOn = pref != null ? Boolean.TRUE.equals(pref.getPush()) : DEFAULT_PUSH;

        LocalDateTime resumeAt = QuietHours.resumeAtOrNull(pref, LocalDateTime.now());
        if (emailOn) deliverOrQueue(NotificationChannelType.EMAIL, request, resumeAt);
        if (pushOn) deliverOrQueue(NotificationChannelType.PUSH, request, resumeAt);
    }

    private void deliverOrQueue(NotificationChannelType ct, NotificationRequest request, LocalDateTime resumeAt) {
        if (resumeAt == null) {
            send(ct, request);
            return;
        }
        pendingRepository.save(PendingNotificationDelivery.builder()
                .recipient(request.getRecipient())
                .channel(ct)
                .type(request.getType())
                .priority(request.getPriority())
                .title(request.getTitle())
                .message(request.getMessage())
                .link(request.getLink())
                .deliverAt(resumeAt)
                .build());
        log.debug("Queued {} for user {} until {}", ct, request.getRecipient().getId(), resumeAt);
    }

    // Package-private so QuietHoursFlushService can re-send via the channel map.
    void sendDirect(NotificationChannelType ct, NotificationRequest request) {
        send(ct, request);
    }

    private void send(NotificationChannelType ct, NotificationRequest request) {
        NotificationChannel channel = channels.get(ct);
        if (channel != null) channel.send(request);
    }
}
