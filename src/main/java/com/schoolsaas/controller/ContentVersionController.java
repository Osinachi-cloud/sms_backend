package com.schoolsaas.controller;

import com.schoolsaas.model.ContentItem;
import com.schoolsaas.model.ContentVersion;
import com.schoolsaas.security.UserPrincipal;
import com.schoolsaas.service.ContentVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cms/content/{contentId}/versions")
@RequiredArgsConstructor
public class ContentVersionController {

    private final ContentVersionService versionService;

    public record CreateVersionRequest(String changeSummary) {}
    public record ScheduleRequest(LocalDateTime publishAt) {}

    @PostMapping
    @PreAuthorize("hasPermission(#contentId, 'Content', 'cms.content.edit')")
    public ResponseEntity<ContentVersion> createVersion(
            @PathVariable UUID contentId,
            @Valid @RequestBody CreateVersionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ContentVersion version = versionService.createVersion(
                contentId,
                userPrincipal.getId(),
                request.changeSummary()
        );
        return ResponseEntity.ok(version);
    }

    @GetMapping
    @PreAuthorize("hasPermission(#contentId, 'Content', 'cms.content.read')")
    public ResponseEntity<Page<ContentVersion>> getVersionHistory(
            @PathVariable UUID contentId,
            Pageable pageable) {
        return ResponseEntity.ok(versionService.getVersionHistory(contentId, pageable));
    }

    @GetMapping("/{versionNumber}")
    @PreAuthorize("hasPermission(#contentId, 'Content', 'cms.content.read')")
    public ResponseEntity<ContentVersion> getVersion(
            @PathVariable UUID contentId,
            @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(versionService.getVersion(contentId, versionNumber));
    }

    @PostMapping("/{versionNumber}/restore")
    @PreAuthorize("hasPermission(#contentId, 'Content', 'cms.content.edit')")
    public ResponseEntity<ContentItem> restoreVersion(
            @PathVariable UUID contentId,
            @PathVariable Integer versionNumber,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ContentItem content = versionService.restoreVersion(
                contentId,
                versionNumber,
                userPrincipal.getId()
        );
        return ResponseEntity.ok(content);
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasPermission(#contentId, 'Content', 'cms.content.publish')")
    public ResponseEntity<ContentItem> schedulePublish(
            @PathVariable UUID contentId,
            @Valid @RequestBody ScheduleRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ContentItem content = versionService.schedulePublish(
                contentId,
                request.publishAt(),
                userPrincipal.getId()
        );
        return ResponseEntity.ok(content);
    }

    @DeleteMapping("/schedule")
    @PreAuthorize("hasPermission(#contentId, 'Content', 'cms.content.publish')")
    public ResponseEntity<ContentItem> cancelSchedule(
            @PathVariable UUID contentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ContentItem content = versionService.cancelSchedule(contentId, userPrincipal.getId());
        return ResponseEntity.ok(content);
    }

    @PostMapping("/featured")
    @PreAuthorize("hasPermission(#contentId, 'Content', 'cms.content.publish')")
    public ResponseEntity<ContentItem> toggleFeatured(
            @PathVariable UUID contentId,
            @RequestParam boolean featured,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ContentItem content = versionService.toggleFeatured(
                contentId,
                featured,
                userPrincipal.getId()
        );
        return ResponseEntity.ok(content);
    }

    @PostMapping("/view")
    public ResponseEntity<Map<String, String>> incrementViewCount(@PathVariable UUID contentId) {
        versionService.incrementViewCount(contentId);
        return ResponseEntity.ok(Map.of("status", "counted"));
    }
}
