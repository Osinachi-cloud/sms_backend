package com.schoolsaas.repository;

import com.schoolsaas.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    List<Attendance> findByStudentIdOrderByDateDesc(UUID studentId);

    List<Attendance> findByStudentIdAndDateBetween(UUID studentId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByClassIdAndDate(UUID classId, LocalDate date);

    Optional<Attendance> findByStudentIdAndDate(UUID studentId, LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.studentId = :studentId AND a.status = :status")
    long countByStudentIdAndStatus(@Param("studentId") UUID studentId, @Param("status") String status);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.studentId = :studentId")
    long countByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT a FROM Attendance a WHERE a.schoolId = :schoolId AND a.studentId = :studentId ORDER BY a.date DESC")
    List<Attendance> findBySchoolIdAndStudentId(@Param("schoolId") UUID schoolId, @Param("studentId") UUID studentId);

    @Query("SELECT COUNT(DISTINCT a.date) FROM Attendance a WHERE a.schoolId = :schoolId AND a.date >= :startDate")
    long countSchoolDaysSince(@Param("schoolId") UUID schoolId, @Param("startDate") LocalDate startDate);
}
