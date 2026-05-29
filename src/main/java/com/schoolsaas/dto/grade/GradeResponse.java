package com.schoolsaas.dto.grade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private UUID termId;
    private String termName;
    private String assessmentType;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String gradeLetter;
    private String remarks;
    private LocalDateTime createdAt;
}
