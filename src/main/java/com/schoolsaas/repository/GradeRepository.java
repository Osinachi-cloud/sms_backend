package com.schoolsaas.repository;

import com.schoolsaas.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {

    List<Grade> findByStudentIdOrderByTermIdDescSubjectIdAsc(UUID studentId);

    List<Grade> findByStudentIdAndTermId(UUID studentId, UUID termId);

    List<Grade> findBySchoolIdAndSubjectIdAndTermId(UUID schoolId, UUID subjectId, UUID termId);

    @Query("SELECT g FROM Grade g WHERE g.schoolId = :schoolId AND g.studentId = :studentId ORDER BY g.createdAt DESC")
    List<Grade> findBySchoolIdAndStudentId(@Param("schoolId") UUID schoolId, @Param("studentId") UUID studentId);

    @Query("SELECT COUNT(DISTINCT g.studentId) FROM Grade g WHERE g.schoolId = :schoolId")
    long countStudentsWithGrades(@Param("schoolId") UUID schoolId);

    @Query("SELECT AVG(g.score) FROM Grade g WHERE g.schoolId = :schoolId AND g.termId = :termId")
    Double getAverageScoreByTermId(@Param("schoolId") UUID schoolId, @Param("termId") UUID termId);
}
