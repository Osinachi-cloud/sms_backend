package com.schoolsaas.dto.reportcard;

import lombok.Data;

import java.util.UUID;

@Data
public class ReportCardEntryDto {
    private UUID subjectId;
    private String subjectName;
    private Double testScore;
    private Double examScore;
    private Double totalScore;
    private String gradeLetter;
    private String remarks;
    private String teacherName;
}
