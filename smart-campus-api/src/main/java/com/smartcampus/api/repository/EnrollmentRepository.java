package com.smartcampus.api.repository;

import com.smartcampus.api.model.Enrollment;
import com.smartcampus.api.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentIdAndSectionId(Long studentId, Long sectionId);

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findBySectionId(Long sectionId);

    List<Enrollment> findBySectionIdAndStatus(Long sectionId, EnrollmentStatus status);

    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    @Query("SELECT COUNT(e) > 0 FROM Enrollment e " +
           "WHERE e.student.id = :studentId " +
           "AND e.section.offering.id = :offeringId " +
           "AND e.status <> com.smartcampus.api.model.EnrollmentStatus.WITHDRAWN")
    boolean existsActiveInOffering(@Param("studentId") Long studentId,
                                   @Param("offeringId") Long offeringId);

    // WAITLISTED and WITHDRAWN don't consume a seat.
    @Query("SELECT COUNT(e) FROM Enrollment e " +
           "WHERE e.section.id = :sectionId " +
           "AND e.status IN (com.smartcampus.api.model.EnrollmentStatus.ENROLLED, " +
           "                 com.smartcampus.api.model.EnrollmentStatus.COMPLETED)")
    long countActiveBySectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT e FROM Enrollment e " +
           "WHERE e.student.id = :studentId " +
           "AND e.status = com.smartcampus.api.model.EnrollmentStatus.COMPLETED " +
           "AND e.gradeReleased = true")
    List<Enrollment> findCompletedWithReleasedGrades(@Param("studentId") Long studentId);

    @Query("SELECT e FROM Enrollment e " +
           "WHERE e.section.lecturer.id = :lecturerId " +
           "AND e.status IN (com.smartcampus.api.model.EnrollmentStatus.ENROLLED, " +
           "                 com.smartcampus.api.model.EnrollmentStatus.COMPLETED)")
    List<Enrollment> findForLecturer(@Param("lecturerId") Long lecturerId);

    @Query("SELECT e FROM Enrollment e WHERE e.section.offering.id = :offeringId")
    List<Enrollment> findByOfferingId(@Param("offeringId") Long offeringId);
}
