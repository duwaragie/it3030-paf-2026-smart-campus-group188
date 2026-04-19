package com.smartcampus.api.service.notification;

import com.smartcampus.api.model.NotificationPriority;
import com.smartcampus.api.model.NotificationType;
import com.smartcampus.api.model.User;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationRequest {
    User recipient;
    NotificationType type;
    NotificationPriority priority;
    String title;
    String message;
    String link;
}
