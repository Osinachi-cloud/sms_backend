package com.schoolsaas.repository;

import com.schoolsaas.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    Page<Teacher> findBySchoolId(UUID schoolId, Pageable pageable);

    @Query("SELECT t FROM Teacher t WHERE t.schoolId = :schoolId AND t.status = 'ACTIVE'")
    Page<Teacher> findActiveBySchoolId(UUID schoolId, Pageable pageable);

    Page<Teacher> findBySchoolIdAndStatus(UUID schoolId, String status, Pageable pageable);

    Optional<Teacher> findBySchoolIdAndEmployeeId(UUID schoolId, String employeeId);

    Optional<Teacher> findBySchoolIdAndUserId(UUID schoolId, UUID userId);

    Optional<Teacher> findByUserId(UUID userId);

    boolean existsBySchoolIdAndEmployeeId(UUID schoolId, String employeeId);

    boolean existsBySchoolIdAndEmail(UUID schoolId, String email);

    @Query("SELECT t FROM Teacher t WHERE t.schoolId = :schoolId AND t.status = 'ACTIVE' AND " +
           "(LOWER(t.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.employeeId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Teacher> searchBySchoolId(UUID schoolId, String search, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.schoolId = :schoolId AND t.status = 'ACTIVE'")
    long countActiveBySchoolId(UUID schoolId);

    long countBySchoolId(UUID schoolId);

    long countBySchoolIdAndStatus(UUID schoolId, String status);

    java.util.List<Teacher> findBySchoolId(UUID schoolId);

    @Modifying
    @Query("UPDATE Teacher t SET t.email = :newEmail WHERE t.email = :oldEmail")
    int updateEmailByEmail(String oldEmail, String newEmail);
}
