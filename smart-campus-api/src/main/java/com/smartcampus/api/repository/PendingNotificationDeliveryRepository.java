package com.smartcampus.api.repository;

import com.smartcampus.api.model.PendingNotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PendingNotificationDeliveryRepository
        extends JpaRepository<PendingNotificationDelivery, Long> {

    List<PendingNotificationDelivery> findByDeliverAtLessThanEqualOrderByDeliverAtAsc(LocalDateTime cutoff);

    List<PendingNotificationDelivery> findByRecipientId(Long recipientId);
}
