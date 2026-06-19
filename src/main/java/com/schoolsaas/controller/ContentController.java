package com.schoolsaas.controller;

import com.schoolsaas.dto.cms.ContentResponse;
import com.schoolsaas.dto.cms.CreateContentRequest;
import com.schoolsaas.model.ContentFolder;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/cms")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping("/folders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ContentFolder>> getFolders(@PathVariable UUID schoolId, Pageable pageable) {
        List<ContentFolder> list = contentService.getRootFolders(schoolId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/folders/by-subject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getFoldersBySubject(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(contentService.getFoldersBySubject(schoolId));
    }

    @PostMapping("/folders")
    @PreAuthorize("hasPermission(#schoolId, 'cms.folder.create') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ContentFolder> createFolder(
            @PathVariable UUID schoolId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = body.get("description") != null ? (String) body.get("description") : null;
        UUID parentId = body.get("parentId") != null ? UUID.fromString((String) body.get("parentId")) : null;
        UUID classId = body.get("classId") != null ? UUID.fromString((String) body.get("classId")) : null;
        UUID subjectId = body.get("subjectId") != null ? UUID.fromString((String) body.get("subjectId")) : null;

        return ResponseEntity.ok(contentService.createFolder(schoolId, name, description, parentId, classId, subjectId));
    }

    @PutMapping("/folders/{folderId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.folder.create') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ContentFolder> updateFolder(
            @PathVariable UUID schoolId,
            @PathVariable UUID folderId,
            @RequestBody Map<String, Object> body) {
        String name = body.get("name") != null ? (String) body.get("name") : null;
        String description = body.get("description") != null ? (String) body.get("description") : null;
        UUID classId = body.get("classId") != null ? UUID.fromString((String) body.get("classId")) : null;
        UUID subjectId = body.get("subjectId") != null ? UUID.fromString((String) body.get("subjectId")) : null;

        return ResponseEntity.ok(contentService.updateFolder(schoolId, folderId, name, description, classId, subjectId));
    }

    @DeleteMapping("/folders/{folderId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.folder.create') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable UUID schoolId,
            @PathVariable UUID folderId) {
        contentService.deleteFolder(schoolId, folderId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/content")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ContentResponse>> getContent(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID studentId,
            Pageable pageable) {
        UUID currentUserId = studentId != null ? studentId : SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(contentService.getContent(schoolId, status, currentUserId, pageable));
    }

    @GetMapping("/content/by-folder/{folderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ContentResponse>> getContentByFolder(
            @PathVariable UUID schoolId,
            @PathVariable UUID folderId,
            @RequestParam(required = false) UUID studentId) {
        UUID currentUserId = studentId != null ? studentId : SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(contentService.getContentsByFolder(schoolId, folderId, currentUserId));
    }

    @GetMapping("/content/pending")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.approve') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<ContentResponse>> getPendingContent(
            @PathVariable UUID schoolId,
            Pageable pageable) {
        return ResponseEntity.ok(contentService.getContent(schoolId, "PENDING", null, pageable));
    }

    @GetMapping("/content/{contentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentResponse> getContentItem(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId,
            @RequestParam(required = false) UUID studentId) {
        UUID currentUserId = studentId != null ? studentId : SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(contentService.getContentItem(schoolId, contentId, currentUserId));
    }

    @PostMapping("/content")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.create') or hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ContentResponse> createContent(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateContentRequest request) {
        return ResponseEntity.ok(contentService.createContent(schoolId, request));
    }

    @PutMapping("/content/{contentId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ContentResponse> updateContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId,
            @Valid @RequestBody CreateContentRequest request) {
        boolean isAdmin = SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin() || SecurityUtils.hasPermission("cms.content.edit.any");
        return ResponseEntity.ok(contentService.updateContent(schoolId, contentId, request, isAdmin));
    }

    @PutMapping("/content/{contentId}/submit")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.submit')")
    public ResponseEntity<ContentResponse> submitForApproval(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId) {
        return ResponseEntity.ok(contentService.submitForApproval(schoolId, contentId));
    }

    @PutMapping("/content/{contentId}/approve")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.approve') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ContentResponse> approveContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId) {
        return ResponseEntity.ok(contentService.approveContent(schoolId, contentId));
    }

    @PutMapping("/content/{contentId}/reject")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.reject') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ContentResponse> rejectContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(contentService.rejectContent(schoolId, contentId, body.get("reason")));
    }

    @DeleteMapping("/content/{contentId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.delete') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteContent(
            @PathVariable UUID schoolId,
            @PathVariable UUID contentId) {
        boolean isAdmin = SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin() || SecurityUtils.hasPermission("cms.content.delete.any");
        contentService.deleteContent(schoolId, contentId, isAdmin);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/upload")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.create') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadFile(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        // Sanitize filename
        String safeName = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = timestamp + "_" + safeName;

        // Store in uploads directory
        Path uploadPath = Paths.get("uploads", schoolId.toString(), "cms");
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        // Return relative URL
        String fileUrl = "/uploads/" + schoolId + "/cms/" + fileName;
        return ResponseEntity.ok(Map.of(
            "url", fileUrl,
            "name", originalFilename,
            "fullUrl", fileUrl
        ));
    }
}
