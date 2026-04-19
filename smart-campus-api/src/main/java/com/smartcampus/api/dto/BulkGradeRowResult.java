package com.smartcampus.api.dto;

import com.smartcampus.api.model.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in the CSV upload result. Covers both validation feedback
 * (dry-run preview) and commit outcome (what was actually applied).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkGradeRowResult {

    /** Row number in the CSV (1-based, excluding header). */
    private int rowNumber;

    private String srn;

    /** Raw grade text from the CSV cell. */
    private String inputGrade;

    /** Resolved student name if SRN matched an enrollment. */
    private String studentName;

    /** Existing grade on the enrollment (to show before → after). */
    private Grade currentGrade;

    /** Parsed grade that will be applied, if valid. */
    private Grade parsedGrade;

    private Status status;

    /** Human-readable error when status is INVALID or SKIPPED. */
    private String error;

    public enum Status {
        /** Valid row; will be applied on commit. */
        VALID,
        /** Empty grade — intentionally skipped (lecturer may fill later). */
        SKIPPED,
        /** Row rejected (bad SRN, bad grade, wrong section, withdrawn enrollment, etc.). */
        INVALID
    }
}
