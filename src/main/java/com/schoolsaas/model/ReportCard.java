package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "term_id")
    private UUID termId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "attendance_present")
    @Builder.Default
    private Integer attendancePresent = 0;

    @Column(name = "attendance_absent")
    @Builder.Default
    private Integer attendanceAbsent = 0;

    @Column(name = "attendance_late")
    @Builder.Default
    private Integer attendanceLate = 0;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "average_score")
    private Double averageScore;

    @Column(name = "overall_grade")
    private String overallGrade;

    @Column(name = "class_position")
    private Integer classPosition;

    @Column(name = "class_size")
    private Integer classSize;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "principal_comment", columnDefinition = "TEXT")
    private String principalComment;

    @Column(nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "generated_pdf_url")
    private String generatedPdfUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
