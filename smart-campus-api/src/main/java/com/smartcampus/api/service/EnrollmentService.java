package com.smartcampus.api.service;

import com.smartcampus.api.dto.BulkGradeResult;
import com.smartcampus.api.dto.BulkGradeRowResult;
import com.smartcampus.api.dto.EnrollmentDTO;
import com.smartcampus.api.dto.GradeChangeDTO;
import com.smartcampus.api.dto.TranscriptDTO;
import com.smartcampus.api.exception.BadRequestException;
import com.smartcampus.api.exception.ResourceNotFoundException;
import com.smartcampus.api.exception.UserNotFoundException;
import com.smartcampus.api.model.*;
import com.smartcampus.api.repository.CourseOfferingRepository;
import com.smartcampus.api.repository.CourseSectionRepository;
import com.smartcampus.api.repository.EnrollmentRepository;
import com.smartcampus.api.repository.GradeChangeRepository;
import com.smartcampus.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final UserRepository userRepository;
    private final GradeChangeRepository gradeChangeRepository;
    private final NotificationService notificationService;

    @Transactional
    public EnrollmentDTO enroll(Long studentId, Long sectionId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));
        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can self-enroll.");
        }

        CourseSection section = courseSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section " + sectionId + " not found"));

        CourseOffering offering = section.getOffering();
        if (offering.getStatus() != CourseOfferingStatus.OPEN) {
            throw new BadRequestException("Enrollment for this course is not open.");
        }

        // Duplicate in this section (WITHDRAWN rows can be re-enrolled).
        enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId)
                .ifPresent(existing -> {
                    if (existing.getStatus() != EnrollmentStatus.WITHDRAWN) {
                        throw new BadRequestException(
                                "You are already enrolled in this section.");
                    }
                });
        if (enrollmentRepository.existsActiveInOffering(studentId, offering.getId())) {
            throw new BadRequestException(
                    "You are already enrolled in another section of " + offering.getCode() + ".");
        }

        List<String> missing = missingPrerequisitesFor(studentId, offering.getPrerequisites());
        if (!missing.isEmpty()) {
            throw new BadRequestException(
                    "Missing prerequisites: " + String.join(", ", missing));
        }

        long active = enrollmentRepository.countActiveBySectionId(sectionId);
        EnrollmentStatus status = active < section.getCapacity()
                ? EnrollmentStatus.ENROLLED
                : EnrollmentStatus.WAITLISTED;

        // Reuse an existing (withdrawn) row to satisfy the unique constraint.
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndSectionId(studentId, sectionId)
                .map(existing -> {
                    existing.setStatus(status);
                    existing.setWithdrawnAt(null);
                    return existing;
                })
                .orElseGet(() -> Enrollment.builder()
                        .student(student)
                        .section(section)
                        .status(status)
                        .gradeReleased(false)
                        .build());

        enrollment = enrollmentRepository.save(enrollment);

        String sectionLabel = section.getLabel();
        String lecturerBlurb = section.getLecturer() != null
                ? " with " + section.getLecturer().getName()
                : "";

        if (status == EnrollmentStatus.ENROLLED) {
            notificationService.notify(student,
                    NotificationType.ENROLLMENT_CONFIRMED,
                    NotificationPriority.MEDIUM,
                    "Enrollment confirmed: " + offering.getCode() + " / " + sectionLabel,
                    "You're enrolled in " + offering.getTitle() + " (" + sectionLabel + ")"
                            + lecturerBlurb + " for " + offering.getSemester() + ".",
                    "/enrollments");
        } else {
            notificationService.notify(student,
                    NotificationType.ENROLLMENT_WAITLISTED,
                    NotificationPriority.MEDIUM,
                    "Waitlisted: " + offering.getCode() + " / " + sectionLabel,
                    "The section is full. You're on the waitlist for " + offering.getTitle()
                            + " (" + sectionLabel + "). We'll notify you if a seat opens up.",
                    "/enrollments");
        }

        return toDTO(enrollment);
    }

    @Transactional
    public EnrollmentDTO withdraw(Long studentId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment " + enrollmentId + " not found"));

        if (!enrollment.getStudent().getId().equals(studentId)) {
            throw new BadRequestException("You can only withdraw your own enrollments.");
        }

        if (enrollment.getStatus() == EnrollmentStatus.WITHDRAWN
                || enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new BadRequestException("This enrollment cannot be withdrawn.");
        }

        boolean heldActiveSeat = enrollment.getStatus() == EnrollmentStatus.ENROLLED;
        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        enrollment.setWithdrawnAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);

        CourseSection section = enrollment.getSection();
        CourseOffering offering = section.getOffering();

        notificationService.notify(enrollment.getStudent(),
                NotificationType.ENROLLMENT_WITHDRAWN,
                NotificationPriority.LOW,
                "Withdrawal confirmed: " + offering.getCode() + " / " + section.getLabel(),
                "You've withdrawn from " + offering.getTitle() + " (" + section.getLabel() + ").",
                "/enrollments");

        if (heldActiveSeat) {
            promoteWaitlist(section);
        }

        return toDTO(enrollment);
    }

    private void promoteWaitlist(CourseSection section) {
        List<Enrollment> waitlist = enrollmentRepository
                .findBySectionIdAndStatus(section.getId(), EnrollmentStatus.WAITLISTED);
        if (waitlist.isEmpty()) return;

        waitlist.sort(Comparator.comparing(Enrollment::getEnrolledAt));
        Enrollment promoted = waitlist.get(0);
        promoted.setStatus(EnrollmentStatus.ENROLLED);
        enrollmentRepository.save(promoted);

        CourseOffering offering = section.getOffering();
        notificationService.notify(promoted.getStudent(),
                NotificationType.WAITLIST_PROMOTED,
                NotificationPriority.HIGH,
                "Seat available: " + offering.getCode() + " / " + section.getLabel(),
                "Good news, a seat opened up in " + offering.getTitle()
                        + " (" + section.getLabel() + ") and you've been moved off the waitlist.",
                "/enrollments");
    }

    public List<EnrollmentDTO> listMine(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .sorted(Comparator.comparing(Enrollment::getEnrolledAt).reversed())
                .map(this::toDTO)
                .toList();
    }

    public List<EnrollmentDTO> listBySection(Long sectionId) {
        return enrollmentRepository.findBySectionId(sectionId).stream()
                .sorted(Comparator.comparing(Enrollment::getEnrolledAt))
                .map(this::toDTO)
                .toList();
    }

    // Post-release edits notify the student; every change is recorded in grade_changes.
    @Transactional
    public EnrollmentDTO setGrade(Long enrollmentId, Grade grade, String reason, User actor) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment " + enrollmentId + " not found"));

        authorizeLecturerOrAdmin(actor, enrollment.getSection().getOffering().getId());

        if (enrollment.getStatus() == EnrollmentStatus.WITHDRAWN) {
            throw new BadRequestException("Cannot grade a withdrawn enrollment.");
        }

        Grade previous = enrollment.getGrade();
        if (previous == grade) return toDTO(enrollment);

        boolean wasReleased = Boolean.TRUE.equals(enrollment.getGradeReleased());
        enrollment.setGrade(grade);
        Enrollment saved = enrollmentRepository.save(enrollment);

        recordGradeChange(saved, previous, grade, wasReleased, reason, actor);

        if (wasReleased) {
            notifyGradeUpdated(saved, previous, grade);
        }

        return toDTO(saved);
    }

    private void recordGradeChange(Enrollment enrollment,
                                   Grade previous,
                                   Grade next,
                                   boolean wasReleased,
                                   String reason,
                                   User actor) {
        gradeChangeRepository.save(GradeChange.builder()
                .enrollment(enrollment)
                .previousGrade(previous)
                .newGrade(next)
                .wasReleased(wasReleased)
                .changedBy(actor)
                .reason(reason != null && !reason.isBlank() ? reason.trim() : null)
                .build());
    }

    private void notifyGradeUpdated(Enrollment enrollment, Grade previous, Grade next) {
        CourseOffering offering = enrollment.getSection().getOffering();
        String from = previous != null ? previous.getLabel() : "—";
        String to = next != null ? next.getLabel() : "—";
        notificationService.notify(enrollment.getStudent(),
                NotificationType.GRADE_UPDATED,
                NotificationPriority.HIGH,
                "Grade updated: " + offering.getCode(),
                "Your grade for " + offering.getTitle()
                        + " (" + enrollment.getSection().getLabel() + ") was updated from "
                        + from + " to " + to + ". Check your transcript.",
                "/transcript");
    }

    @Transactional(readOnly = true)
    public List<GradeChangeDTO> gradeHistory(Long enrollmentId, User actor) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment " + enrollmentId + " not found"));

        boolean isOwner = enrollment.getStudent().getId().equals(actor.getId());
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        boolean isLecturer = actor.getRole() == Role.LECTURER
                && courseSectionRepository.isLecturerOnOffering(
                        enrollment.getSection().getOffering().getId(), actor.getId());
        if (!isOwner && !isAdmin && !isLecturer) {
            throw new BadRequestException(
                    "You don't have permission to view this grade history.");
        }

        return gradeChangeRepository
                .findByEnrollmentIdOrderByChangedAtDesc(enrollmentId)
                .stream()
                .map(this::toHistoryDTO)
                .toList();
    }

    private GradeChangeDTO toHistoryDTO(GradeChange gc) {
        return GradeChangeDTO.builder()
                .id(gc.getId())
                .previousGrade(gc.getPreviousGrade())
                .previousGradeLabel(gc.getPreviousGrade() != null ? gc.getPreviousGrade().getLabel() : null)
                .newGrade(gc.getNewGrade())
                .newGradeLabel(gc.getNewGrade() != null ? gc.getNewGrade().getLabel() : null)
                .wasReleased(gc.getWasReleased())
                .changedByName(gc.getChangedBy() != null ? gc.getChangedBy().getName() : null)
                .reason(gc.getReason())
                .changedAt(gc.getChangedAt())
                .build();
    }

    @Transactional
    public int releaseGradesForOffering(Long offeringId, User actor) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course offering " + offeringId + " not found"));

        authorizeLecturerOrAdmin(actor, offeringId);

        List<Enrollment> enrollments = enrollmentRepository.findByOfferingId(offeringId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED && e.getGrade() != null)
                .filter(e -> !Boolean.TRUE.equals(e.getGradeReleased()))
                .toList();

        if (enrollments.isEmpty()) {
            throw new BadRequestException(
                    "No graded enrollments to release. Grade students first, then release.");
        }

        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (Enrollment e : enrollments) {
            e.setStatus(EnrollmentStatus.COMPLETED);
            e.setGradeReleased(true);
            e.setGradeReleasedAt(now);
            enrollmentRepository.save(e);

            notificationService.notify(e.getStudent(),
                    NotificationType.GRADE_RELEASED,
                    NotificationPriority.HIGH,
                    "Grade released: " + offering.getCode(),
                    "Your grade for " + offering.getTitle()
                            + " (" + e.getSection().getLabel() + ") is now available in your transcript.",
                    "/transcript");
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public String buildGradeCsvTemplate(Long sectionId, User actor) {
        CourseSection section = courseSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section " + sectionId + " not found"));
        authorizeLecturerOrAdmin(actor, section.getOffering().getId());

        List<Enrollment> roster = enrollmentRepository.findBySectionId(sectionId).stream()
                .filter(e -> e.getStatus() != EnrollmentStatus.WITHDRAWN)
                .sorted(Comparator.comparing(e ->
                        e.getStudent().getStudentRegistrationNumber() == null
                                ? ""
                                : e.getStudent().getStudentRegistrationNumber()))
                .toList();

        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
                .builder().setHeader("SRN", "Student Name", "Current Grade", "Grade").build())) {
            for (Enrollment e : roster) {
                String srn = e.getStudent().getStudentRegistrationNumber();
                String name = e.getStudent().getName();
                String current = e.getGrade() != null ? e.getGrade().getLabel() : "";
                printer.printRecord(srn == null ? "" : srn, name, current, "");
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to build CSV template: " + e.getMessage());
        }
        return out.toString();
    }

    // VALID rows are applied; INVALID ones skipped and reported. Extra columns
    // in the CSV (Student Name, Current Grade) are ignored for round-tripping.
    @Transactional
    public BulkGradeResult bulkGradeFromCsv(Long sectionId,
                                            InputStream csvStream,
                                            boolean dryRun,
                                            User actor) {
        CourseSection section = courseSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section " + sectionId + " not found"));
        authorizeLecturerOrAdmin(actor, section.getOffering().getId());

        Map<String, Enrollment> bySrn = new HashMap<>();
        for (Enrollment e : enrollmentRepository.findBySectionId(sectionId)) {
            String srn = e.getStudent().getStudentRegistrationNumber();
            if (srn != null && !srn.isBlank()) {
                bySrn.put(srn.trim().toUpperCase(), e);
            }
        }

        List<BulkGradeRowResult> rows = new ArrayList<>();
        int validCount = 0, skipped = 0, invalid = 0;

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {

            if (!parser.getHeaderMap().keySet().stream()
                    .map(h -> h.trim().toLowerCase()).toList().contains("srn")
                || !parser.getHeaderMap().keySet().stream()
                    .map(h -> h.trim().toLowerCase()).toList().contains("grade")) {
                throw new BadRequestException(
                        "CSV must have 'SRN' and 'Grade' header columns.");
            }

            int rowNum = 0;
            for (CSVRecord record : parser) {
                rowNum++;
                String srn = columnCaseInsensitive(record, "SRN");
                String rawGrade = columnCaseInsensitive(record, "Grade");

                BulkGradeRowResult.BulkGradeRowResultBuilder b = BulkGradeRowResult.builder()
                        .rowNumber(rowNum)
                        .srn(srn)
                        .inputGrade(rawGrade);

                if (srn == null || srn.isBlank()) {
                    rows.add(b.status(BulkGradeRowResult.Status.INVALID)
                            .error("Missing SRN").build());
                    invalid++;
                    continue;
                }

                Enrollment enrollment = bySrn.get(srn.trim().toUpperCase());
                if (enrollment == null) {
                    rows.add(b.status(BulkGradeRowResult.Status.INVALID)
                            .error("No enrollment for SRN '" + srn + "' in this section")
                            .build());
                    invalid++;
                    continue;
                }

                b.studentName(enrollment.getStudent().getName())
                 .currentGrade(enrollment.getGrade());

                if (enrollment.getStatus() == EnrollmentStatus.WITHDRAWN) {
                    rows.add(b.status(BulkGradeRowResult.Status.INVALID)
                            .error("Student has withdrawn from this section").build());
                    invalid++;
                    continue;
                }

                if (rawGrade == null || rawGrade.isBlank()) {
                    rows.add(b.status(BulkGradeRowResult.Status.SKIPPED)
                            .error("Grade cell left blank").build());
                    skipped++;
                    continue;
                }

                Grade parsed = Grade.fromLabel(rawGrade);
                if (parsed == null) {
                    rows.add(b.status(BulkGradeRowResult.Status.INVALID)
                            .error("Unrecognized grade '" + rawGrade + "'").build());
                    invalid++;
                    continue;
                }

                rows.add(b.parsedGrade(parsed)
                        .status(BulkGradeRowResult.Status.VALID)
                        .build());
                validCount++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to parse CSV: " + e.getMessage());
        }

        BulkGradeResult result = BulkGradeResult.builder()
                .total(rows.size())
                .valid(validCount)
                .skipped(skipped)
                .invalid(invalid)
                .rows(rows)
                .committed(false)
                .appliedCount(0)
                .build();

        if (dryRun) {
            return result;
        }

        int applied = 0;
        for (BulkGradeRowResult row : rows) {
            if (row.getStatus() != BulkGradeRowResult.Status.VALID) continue;
            Enrollment enrollment = bySrn.get(row.getSrn().trim().toUpperCase());
            Grade previous = enrollment.getGrade();
            Grade next = row.getParsedGrade();
            if (previous == next) continue;

            boolean wasReleased = Boolean.TRUE.equals(enrollment.getGradeReleased());
            enrollment.setGrade(next);
            Enrollment saved = enrollmentRepository.save(enrollment);
            recordGradeChange(saved, previous, next, wasReleased,
                    "Bulk CSV upload", actor);
            if (wasReleased) notifyGradeUpdated(saved, previous, next);
            applied++;
        }
        result.setCommitted(true);
        result.setAppliedCount(applied);
        return result;
    }

    private static String columnCaseInsensitive(CSVRecord record, String name) {
        for (String header : record.getParser().getHeaderMap().keySet()) {
            if (header.trim().equalsIgnoreCase(name)) {
                String v = record.get(header);
                return v == null ? null : v.trim();
            }
        }
        return null;
    }

    public TranscriptDTO transcript(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));

        List<Enrollment> all = enrollmentRepository.findByStudentId(studentId);
        all.sort(Comparator.comparing(Enrollment::getEnrolledAt).reversed());

        List<Enrollment> gpaEligible = all.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .filter(e -> Boolean.TRUE.equals(e.getGradeReleased()))
                .filter(e -> e.getGrade() != null && e.getGrade().isCountsForGpa())
                .toList();

        double totalPoints = 0.0;
        double totalCredits = 0.0;
        for (Enrollment e : gpaEligible) {
            Double credits = e.getSection().getOffering().getCredits();
            if (credits == null) continue;
            Double points = e.getGrade().getGpaPoints();
            if (points == null) continue;
            totalPoints += points * credits;
            totalCredits += credits;
        }
        double gpa = totalCredits > 0 ? round2(totalPoints / totalCredits) : 0.0;

        return TranscriptDTO.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .studentRegistrationNumber(student.getStudentRegistrationNumber())
                .gpa(gpa)
                .creditsEarned(round2(totalCredits))
                .coursesCompleted(gpaEligible.size())
                .entries(all.stream().map(this::toDTO).toList())
                .build();
    }

    private void authorizeLecturerOrAdmin(User actor, Long offeringId) {
        if (actor.getRole() == Role.ADMIN) return;
        if (actor.getRole() == Role.LECTURER
                && courseSectionRepository.isLecturerOnOffering(offeringId, actor.getId())) {
            return;
        }
        throw new BadRequestException(
                "Only lecturers assigned to this offering (or admins) can perform this action.");
    }

    private List<String> missingPrerequisitesFor(Long studentId, String prerequisites) {
        if (prerequisites == null || prerequisites.isBlank()) return List.of();

        Set<String> required = Arrays.stream(prerequisites.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (required.isEmpty()) return List.of();

        Set<String> passed = enrollmentRepository.findCompletedWithReleasedGrades(studentId).stream()
                .filter(e -> e.getGrade() != null && e.getGrade() != Grade.F
                        && e.getGrade() != Grade.I && e.getGrade() != Grade.W)
                .map(e -> e.getSection().getOffering().getCode())
                .collect(Collectors.toSet());

        return required.stream()
                .filter(code -> !passed.contains(code))
                .toList();
    }

    private EnrollmentDTO toDTO(Enrollment e) {
        CourseSection section = e.getSection();
        CourseOffering offering = section.getOffering();
        return EnrollmentDTO.builder()
                .id(e.getId())
                .studentId(e.getStudent().getId())
                .studentName(e.getStudent().getName())
                .studentEmail(e.getStudent().getEmail())
                .studentRegistrationNumber(e.getStudent().getStudentRegistrationNumber())
                .sectionId(section.getId())
                .sectionLabel(section.getLabel())
                .sectionCapacity(section.getCapacity())
                .lecturerId(section.getLecturer() != null ? section.getLecturer().getId() : null)
                .lecturerName(section.getLecturer() != null ? section.getLecturer().getName() : null)
                .offeringId(offering.getId())
                .courseCode(offering.getCode())
                .courseTitle(offering.getTitle())
                .semester(offering.getSemester())
                .credits(offering.getCredits())
                .status(e.getStatus())
                .grade(e.getGrade())
                .gradeLabel(e.getGrade() != null ? e.getGrade().getLabel() : null)
                .gradePoints(e.getGrade() != null ? e.getGrade().getGpaPoints() : null)
                .gradeReleased(e.getGradeReleased())
                .gradeReleasedAt(e.getGradeReleasedAt())
                .enrolledAt(e.getEnrolledAt())
                .withdrawnAt(e.getWithdrawnAt())
                .build();
    }

    private double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }
}
