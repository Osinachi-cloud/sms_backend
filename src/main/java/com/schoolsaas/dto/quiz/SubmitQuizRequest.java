package com.schoolsaas.dto.quiz;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class SubmitQuizRequest {
    private UUID quizId;
    private UUID studentId;
    private List<Map<String, Object>> answers; // [{"questionId": "...", "answer": "...", "selectedOptions": ["..."]}]
}
