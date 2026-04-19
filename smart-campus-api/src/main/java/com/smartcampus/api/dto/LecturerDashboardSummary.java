package com.smartcampus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerDashboardSummary {
    private int sectionsInCharge;
    private int offeringsInCharge;
    private int totalStudents;
    private int pendingGrades;
    private int gradedPendingRelease;
    private int releasedGrades;
    private long unreadNotifications;
    private List<String> courseCodes;
}
