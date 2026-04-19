package com.smartcampus.api.service.notification;

import com.smartcampus.api.model.PendingNotificationDelivery;
import com.smartcampus.api.repository.PendingNotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Flushes email/push sends that were parked by the dispatcher during a user's
// quiet hours. Runs every minute — matches ScheduledAnnouncementService cadence
// so worst-case delay past quiet-hours-end is ~1 min.
@Slf4j
@Service
@RequiredArgsConstructor
public class QuietHoursFlushService {

    private final PendingNotificationDeliveryRepository pendingRepository;
    private final NotificationDispatcher dispatcher;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void flushDue() {
        List<PendingNotificationDelivery> due =
                pendingRepository.findByDeliverAtLessThanEqualOrderByDeliverAtAsc(LocalDateTime.now());
        if (due.isEmpty()) return;

        for (PendingNotificationDelivery pending : due) {
            try {
                NotificationRequest req = NotificationRequest.builder()
                        .recipient(pending.getRecipient())
                        .type(pending.getType())
                        .priority(pending.getPriority())
                        .title(pending.getTitle())
                        .message(pending.getMessage())
                        .link(pending.getLink())
                        .build();
                dispatcher.sendDirect(pending.getChannel(), req);
            } catch (Exception e) {
                log.error("Failed to flush pending delivery {} ({}): {}",
                        pending.getId(), pending.getChannel(), e.getMessage(), e);
            }
        }
        pendingRepository.deleteAll(due);
        log.info("Flushed {} pending deliveries past quiet hours", due.size());
    }
}
