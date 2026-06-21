package com.schoolsaas.repository;

import com.schoolsaas.model.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, UUID> {
    List<QuizSubmission> findByQuizId(UUID quizId);
    List<QuizSubmission> findByStudentId(UUID studentId);
    List<QuizSubmission> findByQuizIdAndStudentId(UUID quizId, UUID studentId);
    List<QuizSubmission> findByQuizIdAndStudentIdAndStatus(UUID quizId, UUID studentId, String status);
    Optional<QuizSubmission> findFirstByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(UUID quizId, UUID studentId, String status);
    long countByQuizIdAndStudentId(UUID quizId, UUID studentId);
    long countByQuizIdAndStudentIdAndStatusNot(UUID quizId, UUID studentId, String status);
}
