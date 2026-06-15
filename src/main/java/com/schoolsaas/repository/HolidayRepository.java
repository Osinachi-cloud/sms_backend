package com.schoolsaas.repository;

import com.schoolsaas.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    List<Holiday> findBySchoolId(UUID schoolId);

    Page<Holiday> findBySchoolId(UUID schoolId, Pageable pageable);

    @Query("SELECT h FROM Holiday h WHERE h.schoolId = :schoolId AND h.date BETWEEN :startDate AND :endDate")
    List<Holiday> findBySchoolIdAndDateBetween(
            @Param("schoolId") UUID schoolId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsBySchoolIdAndDate(UUID schoolId, LocalDate date);
}
