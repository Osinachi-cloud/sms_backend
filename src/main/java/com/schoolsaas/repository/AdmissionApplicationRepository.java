package com.schoolsaas.repository;

import com.schoolsaas.model.AdmissionApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, UUID> {
    Page<AdmissionApplication> findBySchoolId(UUID schoolId, Pageable pageable);
    Page<AdmissionApplication> findBySchoolIdAndStatus(UUID schoolId, String status, Pageable pageable);
    Optional<AdmissionApplication> findByApplicationNumber(String applicationNumber);
}
