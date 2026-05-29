package com.schoolsaas.repository;

import com.schoolsaas.model.Fee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeeRepository extends JpaRepository<Fee, UUID> {

    Page<Fee> findBySchoolId(UUID schoolId, Pageable pageable);

    List<Fee> findBySchoolIdAndIsActiveTrue(UUID schoolId);

    List<Fee> findBySchoolIdAndClassId(UUID schoolId, UUID classId);

    List<Fee> findBySchoolIdAndTermId(UUID schoolId, UUID termId);

    @Query("SELECT f FROM Fee f WHERE f.schoolId = :schoolId AND f.classId = :classId AND f.isActive = true")
    List<Fee> findActiveBySchoolIdAndClassId(UUID schoolId, UUID classId);

    @Query("SELECT f FROM Fee f WHERE f.schoolId = :schoolId AND f.isMandatory = true AND f.isActive = true")
    List<Fee> findMandatoryBySchoolId(UUID schoolId);
}
