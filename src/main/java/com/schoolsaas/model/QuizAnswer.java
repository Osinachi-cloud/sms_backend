package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    private String answer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> selectedOptions = List.of();

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "marks_obtained", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal marksObtained = BigDecimal.ZERO;

    private String feedback;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
