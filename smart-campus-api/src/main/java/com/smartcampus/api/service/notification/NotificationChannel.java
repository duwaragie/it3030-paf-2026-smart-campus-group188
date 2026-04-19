package com.smartcampus.api.service.notification;

import com.smartcampus.api.model.NotificationChannelType;

public interface NotificationChannel {

    NotificationChannelType type();

    // Implementations must swallow + log per-channel failures so one
    // broken channel can't block the others.
    void send(NotificationRequest request);
}
