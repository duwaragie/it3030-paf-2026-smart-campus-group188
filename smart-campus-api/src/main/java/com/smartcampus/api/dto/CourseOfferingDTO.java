package com.smartcampus.api.dto;

import com.smartcampus.api.model.CourseOfferingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOfferingDTO {
    private Long id;
    private String code;
    private String title;
    private String description;
    private String semester;
    private Double credits;
    private String prerequisites;
    private CourseOfferingStatus status;

    private List<CourseSectionDTO> sections;
    private Integer totalCapacity;
    private Integer totalEnrolled;
    private String lecturerNames;
}
