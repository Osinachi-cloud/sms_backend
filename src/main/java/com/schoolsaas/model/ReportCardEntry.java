package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_card_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_card_id", nullable = false)
    private UUID reportCardId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "test_score", columnDefinition = "DOUBLE PRECISION")
    private BigDecimal testScore;

    @Column(name = "exam_score", columnDefinition = "DOUBLE PRECISION")
    private BigDecimal examScore;

    @Column(name = "total_score", columnDefinition = "DOUBLE PRECISION")
    private BigDecimal totalScore;

    @Column(name = "grade_letter")
    private String gradeLetter;

    private String remarks;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
