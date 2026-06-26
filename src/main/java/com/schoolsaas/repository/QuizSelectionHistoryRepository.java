package com.schoolsaas.repository;

import com.schoolsaas.model.QuizSelectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizSelectionHistoryRepository extends JpaRepository<QuizSelectionHistory, UUID> {
    List<QuizSelectionHistory> findAllByTeacherIdAndSubjectIdAndClassIdOrderByCreatedAtAsc(UUID teacherId, UUID subjectId, UUID classId);
    void deleteAllByTeacherIdAndSubjectIdAndClassId(UUID teacherId, UUID subjectId, UUID classId);
}
