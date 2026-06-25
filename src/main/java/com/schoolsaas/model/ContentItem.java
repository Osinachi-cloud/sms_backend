package com.schoolsaas.model;

import com.schoolsaas.dto.cms.ContentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "content_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "folder_id")
    private UUID folderId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "term_id")
    private UUID termId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "target_class_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<UUID> targetClassIds = List.of();

    @Column(nullable = false)
    private String title;

    @Column(name = "content_type")
    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    @Column(name = "rich_text", columnDefinition = "TEXT")
    private String richText;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "file_urls", columnDefinition = "TEXT[]")
    private List<String> fileUrls;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "video_links", columnDefinition = "TEXT[]")
    private List<String> videoLinks;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    private Integer version = 1;

    @Column(name = "current_version")
    @Builder.Default
    private Integer currentVersion = 1;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private List<String> tags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_audience", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> targetAudience = List.of();

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", insertable = false, updatable = false)
    private ContentFolder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", insertable = false, updatable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private AcademicSession session;
}
