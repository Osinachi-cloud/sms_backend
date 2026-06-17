package com.schoolsaas.dto.quiz;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class QuizDto {
    private UUID id;
    private String title;
    private String description;
    private UUID subjectId;
    private String subjectName;
    private UUID classId;
    private String className;
    private List<UUID> targetClassIds;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
    private BigDecimal passMark;
    private Boolean shuffleQuestions;
    private Boolean showResultsImmediately;
    private Integer maxAttempts;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer questionCount;
    private List<QuizQuestionDto> questions = new java.util.ArrayList<>();
}
