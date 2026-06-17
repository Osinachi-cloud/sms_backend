package com.schoolsaas.repository;

import com.schoolsaas.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {
    List<QuizQuestion> findByQuizIdOrderByOrderIndexAsc(UUID quizId);
    void deleteByQuizId(UUID quizId);
}
