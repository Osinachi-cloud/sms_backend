package com.schoolsaas.dto.quiz;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class QuizAnswerDto {
    private UUID questionId;
    private String questionText;
    private String userAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private BigDecimal marksObtained;
    private BigDecimal totalMarks;
    private String explanation;
}
