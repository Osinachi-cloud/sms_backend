package com.schoolsaas.dto.quiz;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class QuizResultDto {
    private UUID submissionId;
    private UUID quizId;
    private String quizTitle;
    private BigDecimal score;
    private BigDecimal totalMarks;
    private BigDecimal percentage;
    private String gradeLetter;
    private String status;
    private Boolean showCorrectAnswers;
    private List<QuizAnswerDto> answers;
}
