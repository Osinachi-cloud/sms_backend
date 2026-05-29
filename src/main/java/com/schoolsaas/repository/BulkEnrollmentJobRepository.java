package com.schoolsaas.repository;

import com.schoolsaas.model.BulkEnrollmentJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BulkEnrollmentJobRepository extends JpaRepository<BulkEnrollmentJob, UUID> {

    Page<BulkEnrollmentJob> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    Page<BulkEnrollmentJob> findBySchoolIdAndStatus(UUID schoolId, String status, Pageable pageable);
}
