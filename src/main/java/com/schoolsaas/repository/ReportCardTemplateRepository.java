package com.schoolsaas.repository;

import com.schoolsaas.model.ReportCardTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportCardTemplateRepository extends JpaRepository<ReportCardTemplate, UUID> {
    List<ReportCardTemplate> findBySchoolIdAndIsActiveTrue(UUID schoolId);
    Optional<ReportCardTemplate> findBySchoolIdAndIsDefaultTrue(UUID schoolId);
}
