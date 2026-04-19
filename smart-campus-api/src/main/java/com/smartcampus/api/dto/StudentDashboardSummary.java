package com.smartcampus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardSummary {
    private int activeEnrollments;
    private int waitlisted;
    private int coursesCompleted;
    private double gpa;
    private double creditsEarned;
    private long unreadNotifications;
}
