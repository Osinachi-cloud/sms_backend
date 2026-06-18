package com.schoolsaas.repository;

import com.schoolsaas.model.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, UUID> {
    List<TimetableEntry> findBySchoolIdAndClassIdAndIsActiveTrue(UUID schoolId, UUID classId);
    List<TimetableEntry> findBySchoolIdAndTeacherIdAndIsActiveTrue(UUID schoolId, UUID teacherId);
    List<TimetableEntry> findBySchoolIdAndClassIdAndDayOfWeekAndIsActiveTrue(UUID schoolId, UUID classId, Integer dayOfWeek);

    boolean existsBySchoolIdAndClassIdAndPeriodIdAndDayOfWeekAndIsActiveTrue(UUID schoolId, UUID classId, UUID periodId, Integer dayOfWeek);

    Optional<TimetableEntry> findBySchoolIdAndClassIdAndPeriodIdAndDayOfWeekAndIsActiveTrue(UUID schoolId, UUID classId, UUID periodId, Integer dayOfWeek);
}
