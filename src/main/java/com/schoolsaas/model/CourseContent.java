package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "course_contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "class_id")
    private UUID classId;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "target_class_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<UUID> targetClassIds = List.of();

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "content_type")
    private String contentType;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "file_urls", columnDefinition = "TEXT[]")
    private List<String> fileUrls;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "video_links", columnDefinition = "TEXT[]")
    private List<String> videoLinks;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "rich_text", columnDefinition = "TEXT")
    private String richText;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PUBLISHED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;
}
