package com.schoolsaas.repository;

import com.schoolsaas.model.ReportCardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportCardEntryRepository extends JpaRepository<ReportCardEntry, UUID> {
    List<ReportCardEntry> findByReportCardId(UUID reportCardId);
}
