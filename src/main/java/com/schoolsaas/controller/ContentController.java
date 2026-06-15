package com.schoolsaas.controller;

import com.schoolsaas.dto.cms.ContentResponse;
import com.schoolsaas.dto.cms.CreateContentRequest;
import com.schoolsaas.model.ContentFolder;
import com.schoolsaas.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/cms")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping("/folders")
    @PreAuthorize("hasPermission(#schoolId, 'cms.folder.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<ContentFolder>> getFolders(@PathVariable UUID schoolId, Pageable pageable) {
        List<ContentFolder> list = contentService.getRootFolders(schoolId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @PostMapping("/folders")
    @PreAuthorize("hasPermission(#schoolId, 'cms.folder.create')")
    public ResponseEntity<ContentFolder> createFolder(
            @PathVariable UUID schoolId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        UUID parentId = body.get("parentId") != null ? UUID.fromString((String) body.get("parentId")) : null;
        UUID classId = body.get("classId") != null ? UUID.fromString((String) body.get("classId")) : null;
        UUID subjectId = body.get("subjectId") != null ? UUID.fromString((String) body.get("subjectId")) : null;

        return ResponseEntity.ok(contentService.createFolder(schoolId, name, parentId, classId, subjectId));
    }

    @GetMapping("/content")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<ContentResponse>> getContent(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(contentService.getContent(schoolId, status, pageable));
    }

    @GetMapping("/content/pending")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.approve') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<ContentResponse>> getPendingContent(
            @PathVariable UUID schoolId,
            Pageable pageable) {
        return ResponseEntity.ok(contentService.getContent(schoolId, "PENDING", pageable));
    }

    @GetMapping("/content/{contentId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ContentResponse> getContentItem(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId) {
        return ResponseEntity.ok(contentService.getContentItem(schoolId, contentId));
    }

    @PostMapping("/content")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.create')")
    public ResponseEntity<ContentResponse> createContent(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateContentRequest request) {
        return ResponseEntity.ok(contentService.createContent(schoolId, request));
    }

    @PutMapping("/content/{contentId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any')")
    public ResponseEntity<ContentResponse> updateContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId,
            @Valid @RequestBody CreateContentRequest request) {
        return ResponseEntity.ok(contentService.updateContent(schoolId, contentId, request));
    }

    @PutMapping("/content/{contentId}/submit")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.submit')")
    public ResponseEntity<ContentResponse> submitForApproval(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId) {
        return ResponseEntity.ok(contentService.submitForApproval(schoolId, contentId));
    }

    @PutMapping("/content/{contentId}/approve")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.approve')")
    public ResponseEntity<ContentResponse> approveContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId) {
        return ResponseEntity.ok(contentService.approveContent(schoolId, contentId));
    }

    @PutMapping("/content/{contentId}/reject")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.reject')")
    public ResponseEntity<ContentResponse> rejectContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(contentService.rejectContent(schoolId, contentId, body.get("reason")));
    }

    @DeleteMapping("/content/{contentId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.delete')")
    public ResponseEntity<Void> deleteContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId) {
        contentService.deleteContent(schoolId, contentId);
        return ResponseEntity.ok().build();
    }
}
