package com.schoolsaas.service;

import com.schoolsaas.dto.cms.ContentResponse;
import com.schoolsaas.dto.cms.CreateContentRequest;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.ContentFolder;
import com.schoolsaas.model.ContentItem;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.repository.ContentFolderRepository;
import com.schoolsaas.repository.ContentItemRepository;
import com.schoolsaas.repository.TeacherRepository;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentItemRepository contentItemRepository;
    private final ContentFolderRepository contentFolderRepository;
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<ContentFolder> getFolders(UUID schoolId) {
        return contentFolderRepository.findBySchoolIdOrderBySortOrderAsc(schoolId);
    }

    @Transactional(readOnly = true)
    public List<ContentFolder> getRootFolders(UUID schoolId) {
        return contentFolderRepository.findBySchoolIdAndParentIdIsNullOrderBySortOrderAsc(schoolId);
    }

    @Transactional
    public ContentFolder createFolder(UUID schoolId, String name, UUID parentId, UUID classId, UUID subjectId) {
        ContentFolder folder = ContentFolder.builder()
                .schoolId(schoolId)
                .name(name)
                .parentId(parentId)
                .classId(classId)
                .subjectId(subjectId)
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();

        return contentFolderRepository.save(folder);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> getContent(UUID schoolId, String status, Pageable pageable) {
        if ("PENDING".equals(status)) {
            return contentItemRepository.findPendingBySchoolId(schoolId, pageable)
                    .map(ContentResponse::fromEntity);
        }
        if ("PUBLISHED".equals(status) || "APPROVED".equals(status)) {
            return contentItemRepository.findPublishedBySchoolId(schoolId, pageable)
                    .map(ContentResponse::fromEntity);
        }
        if (status != null) {
            return contentItemRepository.findBySchoolIdAndStatus(schoolId, status, pageable)
                    .map(ContentResponse::fromEntity);
        }
        return contentItemRepository.findBySchoolId(schoolId, pageable)
                .map(ContentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ContentResponse getContentItem(UUID schoolId, UUID contentId) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Content", "id", contentId);
        }

        return ContentResponse.fromEntity(content);
    }

    @Transactional
    public ContentResponse createContent(UUID schoolId, CreateContentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);

        ContentItem content = ContentItem.builder()
                .schoolId(schoolId)
                .folderId(request.getFolderId())
                .teacherId(teacher != null ? teacher.getId() : null)
                .title(request.getTitle())
                .contentType(request.getContentType())
                .richText(request.getRichText())
                .fileUrls(request.getFileUrls())
                .videoLinks(request.getVideoLinks())
                .dueDate(request.getDueDate())
                .expiresAt(request.getExpiresAt())
                .status("DRAFT")
                .metadata(request.getMetadata() != null ? request.getMetadata() : new java.util.HashMap<>())
                .build();

        content = contentItemRepository.save(content);
        log.info("Content created: {} in school {}", content.getId(), schoolId);
        return ContentResponse.fromEntity(content);
    }

    @Transactional
    public ContentResponse updateContent(UUID schoolId, UUID contentId, CreateContentRequest request) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Content", "id", contentId);
        }

        if (!"DRAFT".equals(content.getStatus()) && !"REJECTED".equals(content.getStatus())) {
            throw new BadRequestException("Can only edit draft or rejected content");
        }

        content.setTitle(request.getTitle());
        content.setContentType(request.getContentType());
        content.setRichText(request.getRichText());
        content.setFolderId(request.getFolderId());
        content.setFileUrls(request.getFileUrls());
        content.setVideoLinks(request.getVideoLinks());
        content.setDueDate(request.getDueDate());
        content.setExpiresAt(request.getExpiresAt());
        if (request.getMetadata() != null) {
            content.setMetadata(request.getMetadata());
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
    public void deleteContent(UUID schoolId, UUID contentId) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        if (!content.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Content", "id", contentId);
        }

        content.setStatus("ARCHIVED");
        contentItemRepository.save(content);
        log.info("Content deleted (archived): {}", contentId);
    }
}
