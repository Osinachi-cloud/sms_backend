package com.schoolsaas.dto.quiz;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class QuizQuestionDto {
    private UUID id;
    private String questionText;
    private String questionType;
    private List<Map<String, Object>> options;
    private Double marks;
    private Integer orderIndex;
    private String explanation;
}
