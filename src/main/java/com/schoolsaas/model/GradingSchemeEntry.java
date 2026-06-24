package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "grading_scheme_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingSchemeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(name = "source_type", nullable = false)
    private String sourceType; // QUIZ or ASSESSMENT

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(nullable = false)
    @Builder.Default
    private Integer weight = 0; // percentage 0-100

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
