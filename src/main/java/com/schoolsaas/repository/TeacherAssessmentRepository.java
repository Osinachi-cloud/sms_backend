package com.schoolsaas.repository;

import com.schoolsaas.model.TeacherAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherAssessmentRepository extends JpaRepository<TeacherAssessment, UUID> {

    Page<TeacherAssessment> findBySchoolIdAndTeacherId(UUID schoolId, UUID teacherId, Pageable pageable);

    List<TeacherAssessment> findBySchoolIdAndClassIdAndSubjectIdAndTermId(
            UUID schoolId, UUID classId, UUID subjectId, UUID termId);

    @Query("SELECT a FROM TeacherAssessment a WHERE a.schoolId = :schoolId AND a.teacherId = :teacherId AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT(:search, '%')) OR " +
           "LOWER(a.assessmentType) LIKE LOWER(CONCAT(:search, '%')))")
    Page<TeacherAssessment> searchByTeacher(UUID schoolId, UUID teacherId, String search, Pageable pageable);

    @Query("SELECT a FROM TeacherAssessment a WHERE a.schoolId = :schoolId AND a.classId = :classId AND " +
           "a.subjectId = :subjectId AND a.termId = :termId AND a.status = 'PUBLISHED'")
    List<TeacherAssessment> findPublishedByFilters(UUID schoolId, UUID classId, UUID subjectId, UUID termId);

    List<TeacherAssessment> findBySchoolId(UUID schoolId);

    @Query("""
        SELECT a FROM TeacherAssessment a
        WHERE a.schoolId = :schoolId
        AND (:classId IS NULL OR a.classId = :classId)
        AND (:subjectId IS NULL OR a.subjectId = :subjectId)
        AND (:termId IS NULL OR a.termId = :termId)
        AND (:sessionId IS NULL OR a.sessionId = :sessionId)
        ORDER BY a.createdAt DESC
        """)
    List<TeacherAssessment> findBySchoolIdAndFilters(
            @Param("schoolId") UUID schoolId,
            @Param("classId") UUID classId,
            @Param("subjectId") UUID subjectId,
            @Param("termId") UUID termId,
            @Param("sessionId") UUID sessionId);

    long countBySchoolIdAndTeacherId(UUID schoolId, UUID teacherId);
}
