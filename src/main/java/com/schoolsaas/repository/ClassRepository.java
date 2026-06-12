package com.schoolsaas.repository;

import com.schoolsaas.model.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassRepository extends JpaRepository<SchoolClass, UUID> {

    List<SchoolClass> findBySchoolId(UUID schoolId);

    List<SchoolClass> findBySchoolIdAndIsActiveTrue(UUID schoolId);

    List<SchoolClass> findBySchoolIdOrderByGradeLevelAscNameAsc(UUID schoolId);

    @Query("SELECT c FROM SchoolClass c WHERE c.schoolId = :schoolId AND c.isActive = true")
    Page<SchoolClass> findActiveBySchoolId(UUID schoolId, Pageable pageable);

    Optional<SchoolClass> findBySchoolIdAndNameAndSection(UUID schoolId, String name, String section);

    boolean existsBySchoolIdAndNameAndSection(UUID schoolId, String name, String section);

    @Query("SELECT c FROM SchoolClass c WHERE c.schoolId = :schoolId AND c.gradeLevel = :gradeLevel AND c.isActive = true")
    List<SchoolClass> findBySchoolIdAndGradeLevel(UUID schoolId, Integer gradeLevel);

    @Query("SELECT COUNT(c) FROM SchoolClass c WHERE c.schoolId = :schoolId AND c.isActive = true")
    long countActiveBySchoolId(UUID schoolId);

    @Query("SELECT c FROM SchoolClass c WHERE c.schoolId = :schoolId AND c.isActive = true AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.section) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SchoolClass> searchBySchoolId(UUID schoolId, String search, Pageable pageable);

    Page<SchoolClass> findBySchoolId(UUID schoolId, Pageable pageable);

    long countBySchoolId(UUID schoolId);
}
