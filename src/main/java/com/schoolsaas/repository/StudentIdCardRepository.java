package com.schoolsaas.repository;

import com.schoolsaas.model.StudentIdCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentIdCardRepository extends JpaRepository<StudentIdCard, UUID> {
    Page<StudentIdCard> findBySchoolId(UUID schoolId, Pageable pageable);
    Optional<StudentIdCard> findByStudentIdAndStatus(UUID studentId, String status);
}
