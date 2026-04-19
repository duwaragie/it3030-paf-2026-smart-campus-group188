package com.smartcampus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full response from a CSV grade upload — either a dry-run preview or a
 * committed batch, distinguished by the {@code committed} flag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkGradeResult {
    /** Total data rows in the CSV (excluding header). */
    private int total;
    private int valid;
    private int skipped;
    private int invalid;
    /** True if this response reflects a committed write; false for dry-run. */
    private boolean committed;
    /** Count of rows actually written (0 for dry-run). */
    private int appliedCount;
    private List<BulkGradeRowResult> rows;
}
