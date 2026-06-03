package com.schoolsaas.dto.reportcard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ReportCardEntryDto {
    private UUID subjectId;
    private String subjectName;
    private BigDecimal testScore;
    private BigDecimal examScore;
    private BigDecimal totalScore;
    private String gradeLetter;
    private String remarks;
    private String teacherName;
}
