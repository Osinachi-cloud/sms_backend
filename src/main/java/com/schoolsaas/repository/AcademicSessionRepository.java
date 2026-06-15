package com.schoolsaas.repository;

import com.schoolsaas.model.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AcademicSessionRepository extends JpaRepository<AcademicSession, UUID> {

    List<AcademicSession> findBySchoolId(UUID schoolId);

    Page<AcademicSession> findBySchoolId(UUID schoolId, Pageable pageable);

    Optional<AcademicSession> findBySchoolIdAndIsCurrentTrue(UUID schoolId);
}
