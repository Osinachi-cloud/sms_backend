package com.schoolsaas.service;

import com.schoolsaas.dto.cms.ContentResponse;
import com.schoolsaas.dto.cms.CreateContentRequest;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentItemRepository contentItemRepository;
    private final ContentFolderRepository contentFolderRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<ContentFolder> getFolders(UUID schoolId) {
        return contentFolderRepository.findBySchoolIdOrderBySortOrderAsc(schoolId);
    }

    @Transactional(readOnly = true)
    public List<ContentFolder> getRootFolders(UUID schoolId) {
        return contentFolderRepository.findBySchoolIdAndParentIdIsNullOrderBySortOrderAsc(schoolId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFoldersBySubject(UUID schoolId) {
        List<Subject> subjects = subjectRepository.findBySchoolId(schoolId);
        List<ContentFolder> folders = contentFolderRepository.findBySchoolIdOrderBySortOrderAsc(schoolId);

        Map<UUID, List<ContentFolder>> foldersBySubject = folders.stream()
                .filter(f -> f.getSubjectId() != null)
                .collect(Collectors.groupingBy(ContentFolder::getSubjectId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Subject subject : subjects) {
            Map<String, Object> subjectMap = new LinkedHashMap<>();
            subjectMap.put("id", subject.getId());
            subjectMap.put("name", subject.getName());
            subjectMap.put("code", subject.getCode());
            subjectMap.put("folders", foldersBySubject.getOrDefault(subject.getId(), List.of()));
            result.add(subjectMap);
        }

        // Folders without a subject
        List<ContentFolder> unassigned = folders.stream()
                .filter(f -> f.getSubjectId() == null)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("subjects", result);
        response.put("unassignedFolders", unassigned);
        return response;
    }

    @Transactional
    public ContentFolder createFolder(UUID schoolId, String name, String description, UUID parentId, UUID classId, UUID subjectId) {
        ContentFolder folder = ContentFolder.builder()
                .schoolId(schoolId)
                .name(name)
                .description(description)
                .parentId(parentId)
                .classId(classId)
                .subjectId(subjectId)
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();

        return contentFolderRepository.save(folder);
    }

    @Transactional
    public ContentFolder updateFolder(UUID schoolId, UUID folderId, String name, String description, UUID classId, UUID subjectId) {
        ContentFolder folder = contentFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));
        if (!folder.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Folder", "id", folderId);
        }
        if (name != null && !name.isBlank()) {
            folder.setName(name);
        }
        if (description != null) {
            folder.setDescription(description);
        }
        if (classId != null) {
            folder.setClassId(classId);
        }
        if (subjectId != null) {
            folder.setSubjectId(subjectId);
        }
        return contentFolderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(UUID schoolId, UUID folderId) {
        ContentFolder folder = contentFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));
        if (!folder.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Folder", "id", folderId);
        }
        // Move contents out of this folder before deleting
        List<ContentItem> items = contentItemRepository.findByFolderId(folderId);
        for (ContentItem item : items) {
            item.setFolderId(null);
        }
        contentItemRepository.saveAll(items);
        contentFolderRepository.delete(folder);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> getContent(UUID schoolId, String status, UUID currentUserId, Pageable pageable) {
        Page<ContentItem> page;
        if ("PENDING".equals(status)) {
            page = contentItemRepository.findPendingBySchoolId(schoolId, pageable);
        } else if ("PUBLISHED".equals(status) || "APPROVED".equals(status)) {
            page = contentItemRepository.findPublishedBySchoolId(schoolId, pageable);
        } else if (status != null) {
            page = contentItemRepository.findBySchoolIdAndStatus(schoolId, status, pageable);
        } else {
            page = contentItemRepository.findBySchoolId(schoolId, pageable);
        }
        boolean isAdmin = isContentAdmin();
        List<ContentResponse> filtered = page.getContent().stream()
                .filter(c -> isAdmin || isContentVisibleToUser(schoolId, c, currentUserId))
                .map(ContentResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> getContentsByFolder(UUID schoolId, UUID folderId, UUID currentUserId) {
        boolean isAdmin = isContentAdmin();
        return contentItemRepository.findByFolderId(folderId).stream()
                .filter(c -> c.getSchoolId().equals(schoolId))
                .filter(c -> isAdmin || isContentVisibleToUser(schoolId, c, currentUserId))
                .map(ContentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContentResponse getContentItem(UUID schoolId, UUID contentId, UUID currentUserId) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Content", "id", contentId);
        }
        boolean isAdmin = isContentAdmin();
        if (!isAdmin && !isContentVisibleToUser(schoolId, content, currentUserId)) {
            throw new BadRequestException("You do not have access to this content");
        }

        return ContentResponse.fromEntity(content);
    }

    private boolean isContentAdmin() {
        return SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin() || SecurityUtils.hasPermission("cms.content.edit.any");
    }

    private boolean isContentVisibleToUser(UUID schoolId, ContentItem content, UUID currentUserId) {
        List<String> audience = content.getTargetAudience();
        List<UUID> classTargets = content.getTargetClassIds();

        // Admin sees all
        if (isContentAdmin()) {
            return true;
        }

        // If no targeting rules are set, visible to everyone (legacy behaviour)
        if ((audience == null || audience.isEmpty()) && (classTargets == null || classTargets.isEmpty())) {
            return true;
        }

        if (audience != null && audience.contains("ALL")) {
            return true;
        }

        // Check student visibility
        Student student = studentRepository.findByUserId(currentUserId).orElse(null);
        if (student == null) {
            // currentUserId may be a student record ID (passed from frontend as studentId param)
            student = studentRepository.findById(currentUserId).orElse(null);
        }
        if (student != null && student.getSchoolId().equals(schoolId)) {
            if (audience != null && audience.contains("STUDENTS")) {
                return true;
            }
            if (classTargets != null && !classTargets.isEmpty() && classTargets.contains(student.getClassId())) {
                return true;
            }
        }

        // Check teacher visibility
        Teacher teacher = teacherRepository.findByUserId(currentUserId).orElse(null);
        if (teacher != null && teacher.getSchoolId().equals(schoolId)) {
            if (audience != null && audience.contains("TEACHERS")) {
                return true;
            }
            // Teachers can see content they created
            if (content.getTeacherId() != null && content.getTeacherId().equals(teacher.getId())) {
                return true;
            }
        }

        // Check parent visibility
        Parent parent = parentRepository.findByUserId(currentUserId).orElse(null);
        if (parent != null && parent.getSchoolId().equals(schoolId)) {
            if (audience != null && audience.contains("PARENTS")) {
                return true;
            }
        }

        return false;
    }

    @Transactional
    public ContentResponse createContent(UUID schoolId, CreateContentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);

        // Derive subjectId from folder if not provided
        UUID subjectId = request.getSubjectId();
        if (subjectId == null && request.getFolderId() != null) {
            ContentFolder folder = contentFolderRepository.findById(request.getFolderId()).orElse(null);
            if (folder != null) {
                subjectId = folder.getSubjectId();
            }
        }

        ContentItem content = ContentItem.builder()
                .schoolId(schoolId)
                .folderId(request.getFolderId())
                .subjectId(subjectId)
                .termId(request.getTermId())
                .sessionId(request.getSessionId())
                .teacherId(teacher != null ? teacher.getId() : null)
                .title(request.getTitle())
                .contentType(request.getContentType())
                .richText(request.getRichText())
                .fileUrls(request.getFileUrls())
                .videoLinks(request.getVideoLinks())
                .targetClassIds(request.getTargetClassIds() != null ? request.getTargetClassIds() : java.util.List.of())
                .dueDate(request.getDueDate())
                .expiresAt(request.getExpiresAt())
                .status("DRAFT")
                .metadata(request.getMetadata() != null ? request.getMetadata() : new java.util.HashMap<>())
                .body(request.getBody())
                .tags(request.getTags())
                .thumbnailUrl(request.getThumbnailUrl())
                .targetAudience(request.getTargetAudience() != null ? request.getTargetAudience() : java.util.List.of())
                .build();

        content = contentItemRepository.save(content);
        log.info("Content created: {} in school {}", content.getId(), schoolId);
        return ContentResponse.fromEntity(content);
    }

    @Transactional
    public ContentResponse updateContent(UUID schoolId, UUID contentId, CreateContentRequest request, boolean isAdmin) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Content", "id", contentId);
        }

        if (!isAdmin) {
            UUID userId = SecurityUtils.getCurrentUserId();
            Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);
            UUID teacherId = teacher != null ? teacher.getId() : null;
            if (teacherId == null) {
                throw new BadRequestException("You do not have permission to edit this content");
            }
            if (content.getTeacherId() == null || !teacherId.equals(content.getTeacherId())) {
                throw new BadRequestException("You can only edit content you created");
            }
        }

        if (!"DRAFT".equals(content.getStatus()) && !"REJECTED".equals(content.getStatus()) && !isAdmin) {
            throw new BadRequestException("Can only edit draft or rejected content");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            content.setTitle(request.getTitle());
        }
        if (request.getContentType() != null) {
            content.setContentType(request.getContentType());
        }
        if (request.getRichText() != null) {
            content.setRichText(request.getRichText());
        }
        if (request.getFolderId() != null) {
            content.setFolderId(request.getFolderId());
            // Update subject from new folder
            ContentFolder folder = contentFolderRepository.findById(request.getFolderId()).orElse(null);
            if (folder != null && folder.getSubjectId() != null) {
                content.setSubjectId(folder.getSubjectId());
            }
        }
        if (request.getSubjectId() != null) {
            content.setSubjectId(request.getSubjectId());
        }
        if (request.getTermId() != null) {
            content.setTermId(request.getTermId());
        }
        if (request.getSessionId() != null) {
            content.setSessionId(request.getSessionId());
        }
        content.setFileUrls(request.getFileUrls());
        content.setVideoLinks(request.getVideoLinks());
        if (request.getTargetClassIds() != null) {
            content.setTargetClassIds(request.getTargetClassIds());
        }
        if (request.getDueDate() != null) {
            content.setDueDate(request.getDueDate());
        }
        if (request.getExpiresAt() != null) {
            content.setExpiresAt(request.getExpiresAt());
        }
        if (request.getMetadata() != null) {
            content.setMetadata(request.getMetadata());
        }
        if (request.getBody() != null) {
            content.setBody(request.getBody());
        }
        if (request.getTags() != null) {
            content.setTags(request.getTags());
        }
        if (request.getThumbnailUrl() != null) {
            content.setThumbnailUrl(request.getThumbnailUrl());
        }
        content.setVersion(content.getVersion() + 1);

        content = contentItemRepository.save(content);
        log.info("Content updated: {}", contentId);
        return ContentResponse.fromEntity(content);
    }

    @Transactional
    public ContentResponse submitForApproval(UUID schoolId, UUID contentId) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Content", "id", contentId);
        }

        if (!"DRAFT".equals(content.getStatus()) && !"REJECTED".equals(content.getStatus())) {
            throw new BadRequestException("Can only submit draft or rejected content");
        }

        content.setStatus("PENDING");
        content.setRejectionReason(null);
        content = contentItemRepository.save(content);
        log.info("Content submitted for approval: {}", contentId);
        return ContentResponse.fromEntity(content);
    }

    @Transactional
    public ContentResponse approveContent(UUID schoolId, UUID contentId) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Content", "id", contentId);
        }

        if (!"PENDING".equals(content.getStatus())) {
            throw new BadRequestException("Can only approve pending content");
        }

        content.setStatus("APPROVED");
        content.setApprovedBy(SecurityUtils.getCurrentUserId());
        content.setApprovedAt(LocalDateTime.now());
        content.setPublishedAt(LocalDateTime.now());
        content = contentItemRepository.save(content);
        log.info("Content approved: {}", contentId);
        return ContentResponse.fromEntity(content);
    }

    @Transactional
    public ContentResponse rejectContent(UUID schoolId, UUID contentId, String reason) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Content", "id", contentId);
        }

        if (!"PENDING".equals(content.getStatus())) {
            throw new BadRequestException("Can only reject pending content");
        }

        content.setStatus("REJECTED");
        content.setRejectionReason(reason);
        content = contentItemRepository.save(content);
        log.info("Content rejected: {}", contentId);
        return ContentResponse.fromEntity(content);
    }

    @Transactional
    public void deleteContent(UUID schoolId, UUID contentId, boolean isAdmin) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Content", "id", contentId);
        }

        if (!isAdmin) {
            UUID userId = SecurityUtils.getCurrentUserId();
            Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);
            UUID teacherId = teacher != null ? teacher.getId() : null;
            if (teacherId == null) {
                throw new BadRequestException("You do not have permission to delete this content");
            }
            if (content.getTeacherId() == null || !teacherId.equals(content.getTeacherId())) {
                throw new BadRequestException("You can only delete content you created");
            }
        }

        contentItemRepository.delete(content);
        log.info("Content deleted: {}", contentId);
    }
}
