package com.schoolsaas.dto.assessment;

import lombok.Data;

import java.util.UUID;

@Data
public class GradingSchemeEntryDto {
    private UUID id;
    private UUID classId;
    private String className;
    private UUID subjectId;
    private String subjectName;
    private UUID termId;
    private String termName;
    private String sourceType; // QUIZ or ASSESSMENT
    private UUID sourceId;
    private String sourceTitle;
    private Integer weight;
    private Boolean active;
}
