package com.schoolsaas.repository;

import com.schoolsaas.model.SchoolBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolBackupRepository extends JpaRepository<SchoolBackup, UUID> {

    List<SchoolBackup> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    Optional<SchoolBackup> findByDeletionRequestId(UUID deletionRequestId);
}
