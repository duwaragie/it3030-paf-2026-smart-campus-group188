package com.smartcampus.api.service.notification;

import com.smartcampus.api.model.NotificationChannelType;
import com.smartcampus.api.model.UserNotificationPreference;
import com.smartcampus.api.repository.UserNotificationPreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// In-app always fires (can't be disabled — safety net). Email and push are
// gated on the user's preference row; new channels just implement
// NotificationChannel and Spring auto-registers them here.
@Slf4j
@Component
public class NotificationDispatcher {

    private static final boolean DEFAULT_EMAIL = true;
    private static final boolean DEFAULT_PUSH = false;

    private final Map<NotificationChannelType, NotificationChannel> channels =
            new EnumMap<>(NotificationChannelType.class);

    private final UserNotificationPreferenceRepository preferenceRepository;

    public NotificationDispatcher(List<NotificationChannel> channelBeans,
                                  UserNotificationPreferenceRepository preferenceRepository) {
        for (NotificationChannel c : channelBeans) channels.put(c.type(), c);
        this.preferenceRepository = preferenceRepository;
    }

    public void dispatch(NotificationRequest request) {
        Long userId = request.getRecipient().getId();
        send(NotificationChannelType.IN_APP, request);

        UserNotificationPreference pref = preferenceRepository.findByUserId(userId).orElse(null);
        boolean emailOn = pref != null ? Boolean.TRUE.equals(pref.getEmail()) : DEFAULT_EMAIL;
        boolean pushOn = pref != null ? Boolean.TRUE.equals(pref.getPush()) : DEFAULT_PUSH;

        if (emailOn) send(NotificationChannelType.EMAIL, request);
        if (pushOn) send(NotificationChannelType.PUSH, request);
    }

    private void send(NotificationChannelType ct, NotificationRequest request) {
        NotificationChannel channel = channels.get(ct);
        if (channel != null) channel.send(request);
    }
}
