package com.schoolsaas.repository;

import com.schoolsaas.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    Page<Quiz> findBySchoolId(UUID schoolId, Pageable pageable);
    List<Quiz> findBySchoolId(UUID schoolId);
    List<Quiz> findBySchoolIdAndClassIdAndStatus(UUID schoolId, UUID classId, String status);

    @Query("""
        SELECT q FROM Quiz q
        WHERE q.schoolId = :schoolId
        AND (:subjectId IS NULL OR q.subjectId = :subjectId)
        AND (:termId IS NULL OR q.termId = :termId)
        AND (:sessionId IS NULL OR q.sessionId = :sessionId)
        ORDER BY q.createdAt DESC
        """)
    List<Quiz> findBySchoolIdAndFilters(
            @Param("schoolId") UUID schoolId,
            @Param("subjectId") UUID subjectId,
            @Param("termId") UUID termId,
            @Param("sessionId") UUID sessionId);

    List<Quiz> findAllBySchoolIdAndClassIdAndSubjectIdAndCreatedByAndQuizType(UUID schoolId, UUID classId, UUID subjectId, UUID teacherId, String quizType);

    Optional<Quiz> findBySchoolIdAndClassIdAndSubjectIdAndQuizTypeAndIsSelectedForGradeTrue(UUID schoolId, UUID classId, UUID subjectId, String quizType);

    List<Quiz> findBySchoolIdAndSubjectIdAndQuizTypeAndIsSelectedForGradeTrue(UUID schoolId, UUID subjectId, String quizType);
}
