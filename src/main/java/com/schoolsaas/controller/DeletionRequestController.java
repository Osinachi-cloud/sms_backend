package com.schoolsaas.controller;

import com.schoolsaas.model.DeletionRequest;
import com.schoolsaas.security.UserPrincipal;
import com.schoolsaas.service.DeletionRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeletionRequestController {

    private final DeletionRequestService deletionRequestService;

    public record CreateDeletionRequest(@NotBlank String reason) {}
    public record ReviewRequest(@NotBlank String notes, boolean approve) {}
    public record ForwardRequest(@NotBlank String notes) {}
    public record DecisionRequest(@NotBlank String notes, boolean approve) {}

    @PostMapping("/schools/{schoolId}/deletion-request")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'SCHOOL_DELETE')")
    public ResponseEntity<DeletionRequest> createRequest(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateDeletionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        DeletionRequest deletionRequest = deletionRequestService.createRequest(
                schoolId,
                userPrincipal.getId(),
                request.reason()
        );
        return ResponseEntity.ok(deletionRequest);
    }

    @GetMapping("/schools/{schoolId}/deletion-requests")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'SCHOOL_VIEW')")
    public ResponseEntity<Page<DeletionRequest>> getSchoolRequests(
            @PathVariable UUID schoolId,
            Pageable pageable) {
        return ResponseEntity.ok(deletionRequestService.getRequestsBySchool(schoolId, pageable));
    }

    @GetMapping("/admin/deletion-requests")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<DeletionRequest>> getAllRequests(Pageable pageable) {
        return ResponseEntity.ok(deletionRequestService.getAllRequests(pageable));
    }

    @GetMapping("/admin/deletion-requests/pending")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<DeletionRequest>> getPendingRequests(Pageable pageable) {
        return ResponseEntity.ok(deletionRequestService.getPendingRequests(pageable));
    }

    @GetMapping("/admin/deletion-requests/reviewed")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<DeletionRequest>> getReviewedRequests(Pageable pageable) {
        return ResponseEntity.ok(deletionRequestService.getReviewedRequests(pageable));
    }

    @GetMapping("/admin/deletion-requests/forwarded")
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<Page<DeletionRequest>> getForwardedRequests(Pageable pageable) {
        return ResponseEntity.ok(deletionRequestService.getForwardedRequests(pageable));
    }

    @GetMapping("/admin/deletion-requests/{requestId}")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<DeletionRequest> getRequest(@PathVariable UUID requestId) {
        return ResponseEntity.ok(deletionRequestService.getRequest(requestId));
    }

    @PostMapping("/admin/deletion-requests/{requestId}/review")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<DeletionRequest> reviewRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        DeletionRequest deletionRequest = deletionRequestService.reviewRequest(
                requestId,
                userPrincipal.getId(),
                request.notes(),
                request.approve()
        );
        return ResponseEntity.ok(deletionRequest);
    }

    @PostMapping("/admin/deletion-requests/{requestId}/forward")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<DeletionRequest> forwardRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody ForwardRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        DeletionRequest deletionRequest = deletionRequestService.forwardRequest(
                requestId,
                userPrincipal.getId(),
                request.notes()
        );
        return ResponseEntity.ok(deletionRequest);
    }

    @PostMapping("/admin/deletion-requests/{requestId}/decision")
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<DeletionRequest> finalDecision(
            @PathVariable UUID requestId,
            @Valid @RequestBody DecisionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        DeletionRequest deletionRequest = deletionRequestService.finalDecision(
                requestId,
                userPrincipal.getId(),
                request.notes(),
                request.approve()
        );
        return ResponseEntity.ok(deletionRequest);
    }
}
