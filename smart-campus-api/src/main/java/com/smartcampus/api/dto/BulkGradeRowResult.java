package com.smartcampus.api.dto;

import com.smartcampus.api.model.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkGradeRowResult {

    private int rowNumber;
    private String srn;
    private String inputGrade;
    private String studentName;
    private Grade currentGrade;
    private Grade parsedGrade;
    private Status status;
    private String error;

    public enum Status { VALID, SKIPPED, INVALID }
}
