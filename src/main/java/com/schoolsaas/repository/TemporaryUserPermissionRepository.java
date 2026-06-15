package com.schoolsaas.repository;

import com.schoolsaas.model.TemporaryUserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TemporaryUserPermissionRepository extends JpaRepository<TemporaryUserPermission, UUID> {

    List<TemporaryUserPermission> findByUserIdAndSchoolIdAndExpiresAtAfter(UUID userId, UUID schoolId, LocalDateTime now);

    List<TemporaryUserPermission> findByUserIdAndExpiresAtAfter(UUID userId, LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
