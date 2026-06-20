package com.schoolsaas.controller;

import com.schoolsaas.dto.quiz.*;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.create') or hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<QuizDto> createQuiz(@PathVariable UUID schoolId, @RequestBody QuizDto dto) {
        return ResponseEntity.ok(quizService.createQuiz(schoolId, dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<QuizDto>> listQuizzes(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) UUID studentId,
            Pageable pageable) {
        return ResponseEntity.ok(quizService.listQuizzes(schoolId, studentId, pageable));
    }

    @GetMapping("/{quizId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuizDto> getQuiz(
            @PathVariable UUID schoolId,
            @PathVariable UUID quizId,
            @RequestParam(required = false) UUID studentId) {
        return ResponseEntity.ok(quizService.getQuiz(quizId, studentId));
    }

    @PutMapping("/{quizId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<QuizDto> updateQuiz(
            @PathVariable UUID schoolId,
            @PathVariable UUID quizId,
            @RequestBody QuizDto dto) {
        boolean isAdmin = SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin() || SecurityUtils.hasPermission("cms.content.edit.any");
        return ResponseEntity.ok(quizService.updateQuiz(schoolId, quizId, dto, isAdmin));
    }

    @DeleteMapping("/{quizId}")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.delete') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteQuiz(
            @PathVariable UUID schoolId,
            @PathVariable UUID quizId) {
        boolean isAdmin = SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin() || SecurityUtils.hasPermission("cms.content.delete.any");
        quizService.deleteQuiz(schoolId, quizId, isAdmin);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{quizId}/start")
    public ResponseEntity<QuizDto> startQuiz(@PathVariable UUID quizId, @RequestBody Map<String, String> body) {
        String studentIdStr = body != null ? body.get("studentId") : null;
        if (studentIdStr == null || studentIdStr.isBlank()) {
            throw new IllegalArgumentException("studentId is required");
        }
        return ResponseEntity.ok(quizService.startQuiz(quizId, UUID.fromString(studentIdStr)));
    }

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizResultDto> submitQuiz(@PathVariable UUID quizId, @RequestBody SubmitQuizRequest request) {
        UUID studentId = request != null ? request.getStudentId() : null;
        if (studentId == null) {
            List<Map<String, Object>> answers = request != null ? request.getAnswers() : null;
            if (answers != null && !answers.isEmpty() && answers.getFirst() != null && answers.getFirst().get("studentId") != null) {
                studentId = UUID.fromString(answers.getFirst().get("studentId").toString());
            }
        }
        if (studentId == null) {
            throw new IllegalArgumentException("studentId is required");
        }
        return ResponseEntity.ok(quizService.submitQuiz(quizId, studentId, request));
    }

    @GetMapping("/{quizId}/submissions")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<com.schoolsaas.model.QuizSubmission>> getSubmissions(
            @PathVariable UUID schoolId, @PathVariable UUID quizId) {
        return ResponseEntity.ok(quizService.getQuizSubmissions(quizId));
    }

    @GetMapping("/student/{studentId}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<QuizResultDto>> getStudentHistory(
            @PathVariable UUID schoolId, @PathVariable UUID studentId) {
        return ResponseEntity.ok(quizService.getStudentQuizHistory(studentId));
    }

    @PostMapping("/{quizId}/toggle")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<QuizDto> toggleEnabled(
            @PathVariable UUID schoolId, @PathVariable UUID quizId) {
        return ResponseEntity.ok(quizService.toggleQuizEnabled(schoolId, quizId));
    }

    @PostMapping("/{quizId}/add-to-grades")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> addToGrades(
            @PathVariable UUID schoolId, @PathVariable UUID quizId) {
        quizService.addQuizScoreToGrade(schoolId, quizId);
        return ResponseEntity.ok().build();
    }
}
