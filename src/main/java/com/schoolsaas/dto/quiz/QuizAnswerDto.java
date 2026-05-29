package com.schoolsaas.dto.quiz;

import lombok.Data;

import java.util.UUID;

@Data
public class QuizAnswerDto {
    private UUID questionId;
    private String questionText;
    private String userAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private Double marksObtained;
    private Double totalMarks;
    private String explanation;
}
