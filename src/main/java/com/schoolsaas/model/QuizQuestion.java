package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_type", nullable = false)
    @Builder.Default
    private String questionType = "MCQ";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> options = List.of();

    @Column(name = "correct_answer")
    private String correctAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answers", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> correctAnswers = List.of();

    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal marks = BigDecimal.ONE;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
