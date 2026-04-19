package com.smartcampus.api.dto;

import com.smartcampus.api.model.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeChangeDTO {
    private Long id;
    private Grade previousGrade;
    private String previousGradeLabel;
    private Grade newGrade;
    private String newGradeLabel;
    private Boolean wasReleased;
    private String changedByName;
    private String reason;
    private LocalDateTime changedAt;
}
