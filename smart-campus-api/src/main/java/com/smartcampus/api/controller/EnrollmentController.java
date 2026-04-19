package com.smartcampus.api.controller;

import com.smartcampus.api.dto.BulkGradeResult;
import com.smartcampus.api.dto.EnrollRequest;
import com.smartcampus.api.dto.EnrollmentDTO;
import com.smartcampus.api.dto.GradeChangeDTO;
import com.smartcampus.api.dto.SetGradeRequest;
import com.smartcampus.api.dto.TranscriptDTO;
import com.smartcampus.api.exception.BadRequestException;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // POST /api/enrollments  { "sectionId": N }
    @PostMapping("/api/enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentDTO> enroll(
            Authentication authentication,
            @Valid @RequestBody EnrollRequest request) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(user.getId(), request.getSectionId()));
    }

    // DELETE /api/enrollments/{id}
    @DeleteMapping("/api/enrollments/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentDTO> withdraw(
            Authentication authentication,
            @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(enrollmentService.withdraw(user.getId(), id));
    }

    // GET /api/enrollments/me
    @GetMapping("/api/enrollments/me")
    public ResponseEntity<List<EnrollmentDTO>> listMine(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(enrollmentService.listMine(user.getId()));
    }

    // GET /api/enrollments/section/{sectionId}  (LECTURER/ADMIN)
    @GetMapping("/api/enrollments/section/{sectionId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<List<EnrollmentDTO>> roster(@PathVariable Long sectionId) {
        return ResponseEntity.ok(enrollmentService.listBySection(sectionId));
    }

    // PUT /api/enrollments/{id}/grade  (LECTURER on same offering / ADMIN)
    @PutMapping("/api/enrollments/{id}/grade")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<EnrollmentDTO> setGrade(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SetGradeRequest request) {
        User actor = (User) authentication.getPrincipal();
        return ResponseEntity.ok(enrollmentService.setGrade(
                id, request.getGrade(), request.getReason(), actor));
    }

    // GET /api/enrollments/{id}/history  (lecturer on offering, admin, or owning student)
    @GetMapping("/api/enrollments/{id}/history")
    public ResponseEntity<List<GradeChangeDTO>> gradeHistory(
            Authentication authentication,
            @PathVariable Long id) {
        User actor = (User) authentication.getPrincipal();
        return ResponseEntity.ok(enrollmentService.gradeHistory(id, actor));
    }

    // POST /api/course-offerings/{offeringId}/release-grades  (LECTURER on offering / ADMIN)
    @PostMapping("/api/course-offerings/{offeringId}/release-grades")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<Map<String, Integer>> releaseGrades(
            Authentication authentication,
            @PathVariable Long offeringId) {
        User actor = (User) authentication.getPrincipal();
        int released = enrollmentService.releaseGradesForOffering(offeringId, actor);
        return ResponseEntity.ok(Map.of("released", released));
    }

    // GET /api/transcripts/me
    @GetMapping("/api/transcripts/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TranscriptDTO> myTranscript(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(enrollmentService.transcript(user.getId()));
    }

    // GET /api/course-sections/{sectionId}/grades/template — CSV download
    @GetMapping("/api/course-sections/{sectionId}/grades/template")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<byte[]> downloadGradeTemplate(
            Authentication authentication,
            @PathVariable Long sectionId) {
        User actor = (User) authentication.getPrincipal();
        String csv = enrollmentService.buildGradeCsvTemplate(sectionId, actor);
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"grades-section-" + sectionId + ".csv\"")
                .body(body);
    }

    // POST /api/course-sections/{sectionId}/grades/csv?dryRun=bool — multipart upload
    @PostMapping(value = "/api/course-sections/{sectionId}/grades/csv",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<BulkGradeResult> uploadGradeCsv(
            Authentication authentication,
            @PathVariable Long sectionId,
            @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
            @RequestParam("file") MultipartFile file) {
        User actor = (User) authentication.getPrincipal();
        if (file.isEmpty()) {
            throw new BadRequestException("No file uploaded.");
        }
        try {
            return ResponseEntity.ok(enrollmentService.bulkGradeFromCsv(
                    sectionId, file.getInputStream(), dryRun, actor));
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded file: " + e.getMessage());
        }
    }
}
