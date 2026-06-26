package com.schoolsaas.repository;

import com.schoolsaas.model.QuizSelectionPointer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizSelectionPointerRepository extends JpaRepository<QuizSelectionPointer, UUID> {
    Optional<QuizSelectionPointer> findByTeacherIdAndSubjectIdAndClassId(UUID teacherId, UUID subjectId, UUID classId);
}
