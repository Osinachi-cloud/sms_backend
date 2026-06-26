package com.schoolsaas.repository;

import com.schoolsaas.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    List<Subject> findBySchoolIdAndIsActiveTrue(UUID schoolId);

    Page<Subject> findBySchoolIdAndIsActiveTrue(UUID schoolId, Pageable pageable);

    List<Subject> findBySchoolId(UUID schoolId);
    
    @Query(value = "SELECT * FROM subjects WHERE school_id = :schoolId AND class_ids @> CAST(CONCAT('[\"', CAST(:classId AS text), '\"]') AS jsonb)", nativeQuery = true)
    List<Subject> findBySchoolIdAndClassId(UUID schoolId, UUID classId);

    Page<Subject> findBySchoolId(UUID schoolId, Pageable pageable);

    boolean existsByGradingSchemeId(UUID gradingSchemeId);
}
