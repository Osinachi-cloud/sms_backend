package com.schoolsaas.dto.assessment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AssessmentDto {
    private UUID id;
    private String title;
    private String description;
    private UUID subjectId;
    private String subjectName;
    private UUID classId;
    private String className;
    private UUID termId;
    private String termName;
    private UUID sessionId;
    private String sessionName;
    private String assessmentType;
    private BigDecimal maxScore;
    private LocalDateTime dateConducted;
    private String status;
    private Long totalStudents;
    private Long scoredCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
