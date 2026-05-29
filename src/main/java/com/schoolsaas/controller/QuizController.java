package com.schoolsaas.controller;

import com.schoolsaas.dto.quiz.*;
import com.schoolsaas.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<QuizDto> createQuiz(@PathVariable UUID schoolId, @RequestBody QuizDto dto) {
        return ResponseEntity.ok(quizService.createQuiz(schoolId, dto));
    }

    @GetMapping
    public ResponseEntity<Page<QuizDto>> listQuizzes(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(quizService.listQuizzes(schoolId, pageable));
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<QuizDto> getQuiz(@PathVariable UUID quizId) {
        return ResponseEntity.ok(quizService.getQuiz(quizId));
    }

    @PostMapping("/{quizId}/start")
    public ResponseEntity<QuizDto> startQuiz(@PathVariable UUID quizId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(quizService.startQuiz(quizId, UUID.fromString(body.get("studentId"))));
    }

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizResultDto> submitQuiz(@PathVariable UUID quizId, @RequestBody SubmitQuizRequest request) {
        // In real app, get studentId from auth context
        UUID studentId = UUID.fromString(request.getAnswers().isEmpty() ? "" : request.getAnswers().get(0).get("studentId") != null ? request.getAnswers().get(0).get("studentId").toString() : "");
        return ResponseEntity.ok(quizService.submitQuiz(quizId, studentId, request));
    }
}
