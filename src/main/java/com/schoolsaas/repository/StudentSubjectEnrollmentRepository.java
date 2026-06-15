package com.schoolsaas.repository;

import com.schoolsaas.model.StudentSubjectEnrollment;
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
public interface StudentSubjectEnrollmentRepository extends JpaRepository<StudentSubjectEnrollment, UUID> {

    List<StudentSubjectEnrollment> findBySchoolIdAndStudentId(UUID schoolId, UUID studentId);

    Page<StudentSubjectEnrollment> findBySchoolIdAndStudentId(UUID schoolId, UUID studentId, Pageable pageable);

    List<StudentSubjectEnrollment> findBySchoolIdAndSubjectId(UUID schoolId, UUID subjectId);

    Page<StudentSubjectEnrollment> findBySchoolIdAndSubjectId(UUID schoolId, UUID subjectId, Pageable pageable);

    Optional<StudentSubjectEnrollment> findBySchoolIdAndStudentIdAndSubjectId(UUID schoolId, UUID studentId, UUID subjectId);

    long countBySchoolIdAndSubjectIdAndStatus(UUID schoolId, UUID subjectId, String status);

    boolean existsBySchoolIdAndStudentIdAndSubjectId(UUID schoolId, UUID studentId, UUID subjectId);

    @Query("SELECT sse.subjectId FROM StudentSubjectEnrollment sse WHERE sse.schoolId = :schoolId AND sse.studentId = :studentId AND sse.status = 'ENROLLED'")
    List<UUID> findSubjectIdsByStudentId(@Param("schoolId") UUID schoolId, @Param("studentId") UUID studentId);
}
