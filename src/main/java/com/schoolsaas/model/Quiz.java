package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 30;

    @Column(name = "total_marks", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal totalMarks = new BigDecimal("100.00");

    @Column(name = "pass_mark", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal passMark = new BigDecimal("40.00");

    @Column(name = "shuffle_questions")
    @Builder.Default
    private Boolean shuffleQuestions = false;

    @Column(name = "show_results_immediately")
    @Builder.Default
    private Boolean showResultsImmediately = true;

    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 1;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
