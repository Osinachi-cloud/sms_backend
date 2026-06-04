package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quiz_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "started_at")
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private BigDecimal score;

    @Column(name = "total_marks", columnDefinition = "DOUBLE PRECISION")
    private BigDecimal totalMarks;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private BigDecimal percentage;

    @Column(name = "grade_letter")
    private String gradeLetter;

    @Column(nullable = false)
    @Builder.Default
    private String status = "IN_PROGRESS";

    @Column(name = "attempt_number")
    @Builder.Default
    private Integer attemptNumber = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
