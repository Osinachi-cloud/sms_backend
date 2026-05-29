package com.schoolsaas.repository;

import com.schoolsaas.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    Page<Quiz> findBySchoolId(UUID schoolId, Pageable pageable);
    List<Quiz> findBySchoolIdAndClassIdAndStatus(UUID schoolId, UUID classId, String status);
}
