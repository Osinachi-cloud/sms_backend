package com.schoolsaas.repository;

import com.schoolsaas.model.GradingScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradingSchemeRepository extends JpaRepository<GradingScheme, UUID> {
    List<GradingScheme> findAllBySchoolId(UUID schoolId);
    Optional<GradingScheme> findBySchoolIdAndIsDefaultTrue(UUID schoolId);
    boolean existsBySchoolIdAndIsDefaultTrue(UUID schoolId);
}
