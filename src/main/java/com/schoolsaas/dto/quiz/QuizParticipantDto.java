package com.schoolsaas.dto.quiz;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class QuizParticipantDto {
    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private String className;
    private int attemptCount;
    private BigDecimal bestScore;
    private BigDecimal bestPercentage;
    private String bestGradeLetter;
    private boolean passed;
    private List<AttemptInfo> attempts;

    @Data
    public static class AttemptInfo {
        private UUID submissionId;
        private int attemptNumber;
        private BigDecimal score;
        private BigDecimal totalMarks;
        private BigDecimal percentage;
        private String gradeLetter;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
    }
}
