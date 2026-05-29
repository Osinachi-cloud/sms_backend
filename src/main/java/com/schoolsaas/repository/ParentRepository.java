package com.schoolsaas.repository;

import com.schoolsaas.model.Parent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {
    Page<Parent> findBySchoolId(UUID schoolId, Pageable pageable);
    List<Parent> findBySchoolIdAndIsActiveTrue(UUID schoolId);
    Optional<Parent> findByUserId(UUID userId);
}
