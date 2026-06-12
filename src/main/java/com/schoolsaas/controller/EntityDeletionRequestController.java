package com.schoolsaas.controller;

import com.schoolsaas.model.EntityDeletionRequest;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.EntityDeletionRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/entity-deletion-requests")
@RequiredArgsConstructor
public class EntityDeletionRequestController {

    private final EntityDeletionRequestService entityDeletionRequestService;

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'student.delete') or hasPermission(#schoolId, 'teacher.delete') or hasPermission(#schoolId, 'class.delete') or hasPermission(#schoolId, 'user.delete') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<EntityDeletionRequest> createRequest(
            @PathVariable UUID schoolId,
            @RequestBody Map<String, String> body) {
        UUID requestedBy = SecurityUtils.getCurrentUserId();
        String entityType = body.get("entityType");
        UUID entityId = UUID.fromString(body.get("entityId"));
        String reason = body.get("reason");

        return ResponseEntity.ok(entityDeletionRequestService.createRequest(schoolId, requestedBy, entityType, entityId, reason));
    }

    @GetMapping
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<EntityDeletionRequest>> getSchoolRequests(
            @PathVariable UUID schoolId,
            Pageable pageable) {
        return ResponseEntity.ok(entityDeletionRequestService.getRequestsBySchool(schoolId, pageable));
    }

    @PostMapping("/{requestId}/review")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<EntityDeletionRequest> reviewRequest(
            @PathVariable UUID requestId,
            @RequestBody Map<String, Object> body) {
        UUID reviewedBy = SecurityUtils.getCurrentUserId();
        String notes = (String) body.getOrDefault("notes", "");
        boolean approve = Boolean.TRUE.equals(body.get("approve"));

        return ResponseEntity.ok(entityDeletionRequestService.reviewRequest(requestId, reviewedBy, notes, approve));
    }
}
