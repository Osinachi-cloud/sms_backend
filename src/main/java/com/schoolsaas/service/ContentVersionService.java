package com.schoolsaas.service;

import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.ContentItem;
import com.schoolsaas.model.ContentVersion;
import com.schoolsaas.repository.ContentItemRepository;
import com.schoolsaas.repository.ContentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentVersionService {

    private final ContentVersionRepository versionRepository;
    private final ContentItemRepository contentRepository;

    @Transactional
    public ContentVersion createVersion(UUID contentId, UUID userId, String changeSummary) {
        ContentItem content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        Integer maxVersion = versionRepository.findMaxVersionNumber(contentId);
        int newVersionNumber = (maxVersion != null ? maxVersion : 0) + 1;

        ContentVersion version = ContentVersion.builder()
                .contentId(contentId)
                .versionNumber(newVersionNumber)
                .title(content.getTitle())
                .body(content.getBody())
                .createdBy(userId)
                .changeSummary(changeSummary)
                .build();

        content.setCurrentVersion(newVersionNumber);
        contentRepository.save(content);

        log.info("Created version {} for content {}", newVersionNumber, contentId);
        return versionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public Page<ContentVersion> getVersionHistory(UUID contentId, Pageable pageable) {
        return versionRepository.findByContentIdOrderByVersionNumberDesc(contentId, pageable);
    }

    @Transactional(readOnly = true)
    public ContentVersion getVersion(UUID contentId, Integer versionNumber) {
        return versionRepository.findByContentIdAndVersionNumber(contentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("ContentVersion", "version", versionNumber));
    }

    @Transactional
    public ContentItem restoreVersion(UUID contentId, Integer versionNumber, UUID userId) {
        ContentVersion version = getVersion(contentId, versionNumber);
        ContentItem content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        createVersion(contentId, userId, "Before restoring to version " + versionNumber);

        content.setTitle(version.getTitle());
        content.setBody(version.getBody());
        content.setStatus("DRAFT");
        content.setUpdatedBy(userId);

        createVersion(contentId, userId, "Restored from version " + versionNumber);

        log.info("Restored content {} to version {}", contentId, versionNumber);
        return contentRepository.save(content);
    }

    @Transactional
    public ContentItem schedulePublish(UUID contentId, LocalDateTime publishAt, UUID userId) {
        ContentItem content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        content.setScheduledPublishAt(publishAt);
        content.setStatus("SCHEDULED");
        content.setUpdatedBy(userId);

        log.info("Content {} scheduled to publish at {}", contentId, publishAt);
        return contentRepository.save(content);
    }

    @Transactional
    public ContentItem cancelSchedule(UUID contentId, UUID userId) {
        ContentItem content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        content.setScheduledPublishAt(null);
        content.setStatus("DRAFT");
        content.setUpdatedBy(userId);

        log.info("Cancelled scheduled publish for content {}", contentId);
        return contentRepository.save(content);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processScheduledContent() {
        LocalDateTime now = LocalDateTime.now();
        List<ContentItem> scheduledContent = contentRepository.findByStatusAndScheduledPublishAtBefore("SCHEDULED", now);

        for (ContentItem content : scheduledContent) {
            try {
                content.setStatus("PUBLISHED");
                content.setPublishedAt(now);
                content.setScheduledPublishAt(null);
                contentRepository.save(content);
                log.info("Auto-published content {} at scheduled time", content.getId());
            } catch (Exception e) {
                log.error("Failed to auto-publish content {}", content.getId(), e);
            }
        }
    }

    @Transactional
    public ContentItem toggleFeatured(UUID contentId, boolean featured, UUID userId) {
        ContentItem content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        content.setFeatured(featured);
        content.setUpdatedBy(userId);

        log.info("Content {} featured status set to {}", contentId, featured);
        return contentRepository.save(content);
    }

    @Transactional
    public void incrementViewCount(UUID contentId) {
        ContentItem content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));
        content.setViewCount(content.getViewCount() + 1);
        contentRepository.save(content);
    }
}
