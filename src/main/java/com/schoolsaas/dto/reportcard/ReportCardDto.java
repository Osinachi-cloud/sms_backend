package com.schoolsaas.dto.reportcard;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ReportCardDto {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private UUID termId;
    private String termName;
    private Integer attendancePresent;
    private Integer attendanceAbsent;
    private Integer attendanceLate;
    private BigDecimal totalScore;
    private BigDecimal averageScore;
    private String overallGrade;
    private Integer classPosition;
    private Integer classSize;
    private String teacherComment;
    private String principalComment;
    private String status;
    private String generatedPdfUrl;
    private LocalDateTime publishedAt;
    private List<ReportCardEntryDto> entries;
}
