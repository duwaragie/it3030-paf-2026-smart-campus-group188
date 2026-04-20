package com.smartcampus.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

// Only email + push are toggleable; in-app is always on and not stored.
@Entity
@Table(name = "user_notification_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Boolean email = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean push = false;

    // Nullable so Hibernate `ddl-auto: update` can add these columns to a table
    // with existing rows. Service layer coalesces nulls to the defaults below.
    @Builder.Default
    private Boolean quietHoursEnabled = false;

    @Builder.Default
    private LocalTime quietHoursStart = LocalTime.of(22, 0);

    @Builder.Default
    private LocalTime quietHoursEnd = LocalTime.of(7, 0);
}
