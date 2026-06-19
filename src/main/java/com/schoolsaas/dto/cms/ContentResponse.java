package com.schoolsaas.dto.cms;

import com.schoolsaas.model.ContentItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {
    private UUID id;
    private String title;
    private String contentType;
    private UUID folderId;
    private String folderName;
    private UUID subjectId;
    private String subjectName;
    private UUID termId;
    private String termName;
    private UUID sessionId;
    private String sessionName;
    private UUID teacherId;
    private String teacherName;
    private List<java.util.UUID> targetClassIds;
    private String richText;
    private List<String> fileUrls;
    private List<String> videoLinks;
    private LocalDateTime dueDate;
    private String status;
    private String rejectionReason;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private Integer version;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String body;
    private List<String> tags;
    private String thumbnailUrl;
    private List<String> targetAudience;

    public static ContentResponse fromEntity(ContentItem content) {
        return ContentResponse.builder()
                .id(content.getId())
                .title(content.getTitle())
                .contentType(content.getContentType())
                .folderId(content.getFolderId())
                .folderName(content.getFolder() != null ? content.getFolder().getName() : null)
                .subjectId(content.getSubjectId())
                .subjectName(content.getSubject() != null ? content.getSubject().getName() : null)
                .termId(content.getTermId())
                .termName(content.getTerm() != null ? content.getTerm().getName() : null)
                .sessionId(content.getSessionId())
                .sessionName(content.getSession() != null ? content.getSession().getName() : null)
                .teacherId(content.getTeacherId())
                .teacherName(content.getTeacher() != null ? content.getTeacher().getFullName() : null)
                .targetClassIds(content.getTargetClassIds())
                .richText(content.getRichText())
                .fileUrls(content.getFileUrls())
                .videoLinks(content.getVideoLinks())
                .dueDate(content.getDueDate())
                .status(content.getStatus())
                .rejectionReason(content.getRejectionReason())
                .approvedAt(content.getApprovedAt())
                .publishedAt(content.getPublishedAt())
                .expiresAt(content.getExpiresAt())
                .version(content.getVersion())
                .metadata(content.getMetadata())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .body(content.getBody())
                .tags(content.getTags())
                .thumbnailUrl(content.getThumbnailUrl())
                .targetAudience(content.getTargetAudience())
                .build();
    }
}
