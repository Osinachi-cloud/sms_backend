package com.schoolsaas.repository;

import com.schoolsaas.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Page<Student> findBySchoolIdAndStatus(UUID schoolId, String status, Pageable pageable);

    Page<Student> findBySchoolId(UUID schoolId, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.schoolId = :schoolId AND s.status = 'ACTIVE'")
    Page<Student> findActiveBySchoolId(UUID schoolId, Pageable pageable);

    Optional<Student> findBySchoolIdAndAdmissionNumber(UUID schoolId, String admissionNumber);

    Optional<Student> findBySchoolIdAndEmail(UUID schoolId, String email);

    boolean existsBySchoolIdAndAdmissionNumber(UUID schoolId, String admissionNumber);

    boolean existsBySchoolIdAndEmail(UUID schoolId, String email);

    List<Student> findBySchoolIdAndClassId(UUID schoolId, UUID classId);

    @Query("SELECT s FROM Student s WHERE s.schoolId = :schoolId AND s.classId = :classId AND s.status = 'ACTIVE'")
    List<Student> findActiveBySchoolIdAndClassId(UUID schoolId, UUID classId);

    /**
     * Prefix search on student name, admission number and email.
     * Uses <code>search%</code> so a B-tree index on the column can be used.
     * For true substring search (e.g. <code>%search%</code>) add a PostgreSQL
     * <code>pg_trgm</code> GIN index instead.
     */
    @Query("SELECT s FROM Student s WHERE s.schoolId = :schoolId AND s.status = 'ACTIVE' AND " +
           "(LOWER(s.fullName) LIKE LOWER(CONCAT(:search, '%')) OR " +
           "LOWER(s.admissionNumber) LIKE LOWER(CONCAT(:search, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT(:search, '%')))")
    Page<Student> searchBySchoolId(UUID schoolId, String search, Pageable pageable);

    /**
     * Prefix search on student name, admission number and email (class-scoped).
     * Uses <code>search%</code> so a B-tree index on the column can be used.
     * For true substring search add a PostgreSQL <code>pg_trgm</code> GIN index.
     */
    @Query("SELECT s FROM Student s WHERE s.schoolId = :schoolId AND s.classId = :classId AND s.status = 'ACTIVE' AND " +
           "(LOWER(s.fullName) LIKE LOWER(CONCAT(:search, '%')) OR " +
           "LOWER(s.admissionNumber) LIKE LOWER(CONCAT(:search, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT(:search, '%')))")
    Page<Student> searchBySchoolIdAndClassId(UUID schoolId, UUID classId, String search, Pageable pageable);

    Page<Student> findBySchoolIdAndClassIdAndStatus(UUID schoolId, UUID classId, String status, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.schoolId = :schoolId AND s.classId = :classId AND s.status = 'ACTIVE'")
    Page<Student> findActiveBySchoolIdAndClassId(UUID schoolId, UUID classId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.schoolId = :schoolId AND s.status = 'ACTIVE'")
    long countActiveBySchoolId(UUID schoolId);

    long countBySchoolIdAndStatus(UUID schoolId, String status);

    List<Student> findBySchoolId(UUID schoolId);

    List<Student> findTop5BySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.schoolId = :schoolId AND s.classId = :classId AND s.status = 'ACTIVE'")
    long countBySchoolIdAndClassId(UUID schoolId, UUID classId);

    @Query("SELECT MAX(CAST(SUBSTRING(s.admissionNumber, LENGTH(:prefix) + 1) AS integer)) " +
           "FROM Student s WHERE s.schoolId = :schoolId AND s.admissionNumber LIKE CONCAT(:prefix, '%')")
    Integer findMaxAdmissionNumberSequence(UUID schoolId, String prefix);

    long countBySchoolId(UUID schoolId);

    long countByClassId(UUID classId);

    Optional<Student> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE Student s SET s.email = :newEmail WHERE s.email = :oldEmail")
    int updateEmailByEmail(String oldEmail, String newEmail);

    @Modifying
    @Query("UPDATE Student s SET s.parentEmail = :newEmail WHERE s.parentEmail = :oldEmail")
    int updateParentEmailByParentEmail(String oldEmail, String newEmail);
}
