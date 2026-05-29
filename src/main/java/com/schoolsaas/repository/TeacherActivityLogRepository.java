package com.schoolsaas.repository;

import com.schoolsaas.model.TeacherActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherActivityLogRepository extends JpaRepository<TeacherActivityLog, UUID> {
    Page<TeacherActivityLog> findBySchoolIdAndTeacherIdOrderByCreatedAtDesc(UUID schoolId, UUID teacherId, Pageable pageable);
    List<TeacherActivityLog> findBySchoolIdAndTeacherIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID schoolId, UUID teacherId, java.time.LocalDateTime after);
    List<TeacherActivityLog> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);
}
