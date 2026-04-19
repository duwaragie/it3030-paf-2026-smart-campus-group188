package com.smartcampus.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Append-only audit row. A correction creates a new row — never updated in place.
@Entity
@Table(name = "grade_changes",
        indexes = {
                @Index(name = "idx_grade_change_enrollment",
                       columnList = "enrollment_id, changedAt"),
                @Index(name = "idx_grade_change_changed_at",
                       columnList = "changedAt")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Grade previousGrade;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Grade newGrade;

    @Column(nullable = false)
    @Builder.Default
    private Boolean wasReleased = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
