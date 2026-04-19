package com.smartcampus.api.service.notification;

import com.smartcampus.api.dto.NotificationDTO;
import com.smartcampus.api.model.Notification;
import com.smartcampus.api.model.NotificationChannelType;
import com.smartcampus.api.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.IN_APP;
    }

    @Override
    @Transactional
    public void send(NotificationRequest request) {
        try {
            Notification saved = notificationRepository.save(Notification.builder()
                    .recipient(request.getRecipient())
                    .type(request.getType())
                    .priority(request.getPriority())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .link(request.getLink())
                    .read(false)
                    .build());

            NotificationDTO dto = NotificationDTO.builder()
                    .id(saved.getId())
                    .type(saved.getType())
                    .priority(saved.getPriority())
                    .title(saved.getTitle())
                    .message(saved.getMessage())
                    .link(saved.getLink())
                    .read(saved.getRead())
                    .readAt(saved.getReadAt())
                    .createdAt(saved.getCreatedAt())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    request.getRecipient().getId().toString(),
                    "/queue/notifications",
                    dto);
        } catch (Exception e) {
            log.warn("In-app notification failed for user {}: {}",
                    request.getRecipient().getId(), e.getMessage());
        }
    }
}
