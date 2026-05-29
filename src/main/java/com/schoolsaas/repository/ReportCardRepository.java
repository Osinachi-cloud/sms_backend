package com.schoolsaas.repository;

import com.schoolsaas.model.ReportCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportCardRepository extends JpaRepository<ReportCard, UUID> {
    Page<ReportCard> findBySchoolId(UUID schoolId, Pageable pageable);
    List<ReportCard> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    Optional<ReportCard> findByStudentIdAndTermId(UUID studentId, UUID termId);
}
