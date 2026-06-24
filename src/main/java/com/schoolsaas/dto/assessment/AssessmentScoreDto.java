package com.schoolsaas.dto.assessment;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AssessmentScoreDto {
    private UUID id;
    private UUID assessmentId;
    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private BigDecimal score;
    private String remarks;
}
