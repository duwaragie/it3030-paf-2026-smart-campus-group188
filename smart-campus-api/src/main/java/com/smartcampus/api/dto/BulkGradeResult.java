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
public class BulkGradeResult {
    private int total;
    private int valid;
    private int skipped;
    private int invalid;
    private boolean committed;
    private int appliedCount;
    private List<BulkGradeRowResult> rows;
}
