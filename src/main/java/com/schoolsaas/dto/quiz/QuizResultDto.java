package com.schoolsaas.dto.quiz;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class QuizResultDto {
    private UUID submissionId;
    private UUID quizId;
    private String quizTitle;
    private Double score;
    private Double totalMarks;
    private Double percentage;
    private String gradeLetter;
    private String status;
    private List<QuizAnswerDto> answers;
}
