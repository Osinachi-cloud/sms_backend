package com.schoolsaas.repository;

import com.schoolsaas.model.TimetablePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimetablePeriodRepository extends JpaRepository<TimetablePeriod, UUID> {
    List<TimetablePeriod> findBySchoolIdAndIsActiveTrueOrderByPeriodOrderAsc(UUID schoolId);
}
