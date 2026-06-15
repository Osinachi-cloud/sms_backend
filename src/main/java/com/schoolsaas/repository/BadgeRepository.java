package com.schoolsaas.repository;

import com.schoolsaas.model.Badge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {
    List<Badge> findBySchoolIdAndIsActiveTrue(UUID schoolId);

    Page<Badge> findBySchoolIdAndIsActiveTrue(UUID schoolId, Pageable pageable);
}
