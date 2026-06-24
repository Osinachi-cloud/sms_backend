package com.schoolsaas.repository;

import com.schoolsaas.model.AssessmentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentScoreRepository extends JpaRepository<AssessmentScore, UUID> {

    List<AssessmentScore> findByAssessmentId(UUID assessmentId);

    List<AssessmentScore> findByAssessmentIdIn(List<UUID> assessmentIds);

    Optional<AssessmentScore> findByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);

    @Query("SELECT s FROM AssessmentScore s JOIN FETCH s.student WHERE s.assessmentId = :assessmentId")
    List<AssessmentScore> findByAssessmentIdWithStudent(UUID assessmentId);

    long countByAssessmentId(UUID assessmentId);

    @Query("SELECT COUNT(s) FROM AssessmentScore s WHERE s.assessmentId = :assessmentId AND s.score IS NOT NULL")
    long countScoredByAssessmentId(UUID assessmentId);
}
