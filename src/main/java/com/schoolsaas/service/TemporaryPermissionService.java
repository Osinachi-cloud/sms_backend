package com.schoolsaas.service;

import com.schoolsaas.model.TemporaryUserPermission;
import com.schoolsaas.repository.TemporaryUserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemporaryPermissionService {

    private final TemporaryUserPermissionRepository temporaryUserPermissionRepository;

    @Transactional(readOnly = true)
    public List<TemporaryUserPermission> getActivePermissions(UUID userId, UUID schoolId) {
        return temporaryUserPermissionRepository.findByUserIdAndSchoolIdAndExpiresAtAfter(userId, schoolId, LocalDateTime.now());
    }

    @Transactional
    public TemporaryUserPermission grantPermission(UUID userId, UUID schoolId, String permissionKey, UUID grantedBy, LocalDateTime expiresAt) {
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(1);
        }
        TemporaryUserPermission permission = TemporaryUserPermission.builder()
                .userId(userId)
                .schoolId(schoolId)
                .permissionKey(permissionKey)
                .grantedBy(grantedBy)
                .expiresAt(expiresAt)
                .build();
        return temporaryUserPermissionRepository.save(permission);
    }

    @Transactional
    public void revokePermission(UUID permissionId) {
        temporaryUserPermissionRepository.deleteById(permissionId);
    }

    @Transactional
    public void cleanupExpiredPermissions() {
        temporaryUserPermissionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
