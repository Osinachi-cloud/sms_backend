package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teacher_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "term_id")
    private UUID termId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "assessment_type", nullable = false)
    @Builder.Default
    private String assessmentType = "TEST"; // CA, TEST, EXAM, QUIZ

    @Column(name = "max_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal maxScore = new BigDecimal("100.00");

    @Column(name = "date_conducted")
    private LocalDateTime dateConducted;

    @Column(nullable = false)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, PUBLISHED, CLOSED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
