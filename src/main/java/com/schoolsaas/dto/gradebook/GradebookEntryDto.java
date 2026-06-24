package com.schoolsaas.dto.gradebook;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GradebookEntryDto {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private UUID classId;
    private String className;
    private UUID subjectId;
    private String subjectName;
    private UUID termId;
    private String termName;
    private UUID sessionId;
    private String sessionName;
    private UUID sourceId;
    private String sourceTitle;
    private String sourceType;
    private String assessmentType;
    private BigDecimal score;
    private BigDecimal maxScore;
    private BigDecimal percentage;
    private String gradeLetter;
    private String remarks;
    private LocalDateTime createdAt;
}
