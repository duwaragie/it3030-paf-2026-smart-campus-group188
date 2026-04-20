package com.smartcampus.api.repository;

import com.smartcampus.api.model.GradeChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeChangeRepository extends JpaRepository<GradeChange, Long> {

    List<GradeChange> findByEnrollmentIdOrderByChangedAtDesc(Long enrollmentId);
}
