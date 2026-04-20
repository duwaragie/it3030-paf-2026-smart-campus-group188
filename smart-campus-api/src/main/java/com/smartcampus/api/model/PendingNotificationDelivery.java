package com.smartcampus.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Queued email/push sends that were suppressed by a recipient's quiet hours.
// Flushed by QuietHoursFlushService once deliverAt has passed. In-app is never
// queued — it's persisted immediately so the bell stays accurate.
@Entity
@Table(name = "pending_notification_deliveries",
        indexes = @Index(name = "idx_pending_deliver_at", columnList = "deliverAt"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingNotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationChannelType channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationPriority priority;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 500)
    private String link;

    @Column(nullable = false)
    private LocalDateTime deliverAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
