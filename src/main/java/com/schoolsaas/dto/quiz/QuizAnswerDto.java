package com.schoolsaas.dto.quiz;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class QuizAnswerDto {
    private UUID questionId;
    private String questionText;
    private String userAnswer;
    private List<String> selectedOptions = List.of();
    private String correctAnswer;
    private List<String> correctAnswers = List.of();
    private Boolean isCorrect;
    private BigDecimal marksObtained;
    private BigDecimal totalMarks;
    private String explanation;
}
