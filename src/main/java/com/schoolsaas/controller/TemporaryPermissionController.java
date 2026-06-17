package com.schoolsaas.controller;

import com.schoolsaas.model.TemporaryUserPermission;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.TemporaryPermissionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/temporary-permissions")
@RequiredArgsConstructor
public class TemporaryPermissionController {

    private final TemporaryPermissionService temporaryPermissionService;

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('APP_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<TemporaryUserPermission>> getUserTemporaryPermissions(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(temporaryPermissionService.getActivePermissions(userId, schoolId));
    }

    @PostMapping("/users/{userId}")
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<TemporaryUserPermission> grantTemporaryPermission(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId,
            @RequestBody GrantPermissionRequest request) {
        UUID grantedBy = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(temporaryPermissionService.grantPermission(
                userId, schoolId, request.getPermissionKey(), grantedBy, request.getExpiresAt()));
    }

    @DeleteMapping("/{permissionId}")
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<Void> revokeTemporaryPermission(
            @PathVariable UUID schoolId,
            @PathVariable UUID permissionId) {
        temporaryPermissionService.revokePermission(permissionId);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class GrantPermissionRequest {
        private String permissionKey;
        private LocalDateTime expiresAt;
    }
}
