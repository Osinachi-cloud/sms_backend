package com.schoolsaas.repository;

import com.schoolsaas.model.IdCardTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdCardTemplateRepository extends JpaRepository<IdCardTemplate, UUID> {
    List<IdCardTemplate> findBySchoolIdAndIsActiveTrue(UUID schoolId);

    Page<IdCardTemplate> findBySchoolIdAndIsActiveTrue(UUID schoolId, Pageable pageable);

    Optional<IdCardTemplate> findBySchoolIdAndIsDefaultTrue(UUID schoolId);
}
