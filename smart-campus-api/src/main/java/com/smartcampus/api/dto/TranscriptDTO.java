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
public class TranscriptDTO {
    private Long studentId;
    private String studentName;
    private String studentRegistrationNumber;

    private Double gpa;
    private Double creditsEarned;
    private Integer coursesCompleted;
    private List<EnrollmentDTO> entries;
}
