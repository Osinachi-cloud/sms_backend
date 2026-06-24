package com.schoolsaas.controller;

import com.schoolsaas.dto.quiz.*;
import com.schoolsaas.repository.StudentRepository;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final StudentRepository studentRepository;

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
        UUID effectiveStudentId = resolveStudentId(schoolId, studentId);
        return ResponseEntity.ok(quizService.listQuizzes(schoolId, effectiveStudentId, pageable));
    }

    @GetMapping("/{quizId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuizDto> getQuiz(
            @PathVariable UUID schoolId,
            @PathVariable UUID quizId,
            @RequestParam(required = false) UUID studentId) {
        UUID effectiveStudentId = resolveStudentId(schoolId, studentId);
        return ResponseEntity.ok(quizService.getQuiz(quizId, effectiveStudentId));
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
    public ResponseEntity<QuizDto> startQuiz(
            @PathVariable UUID schoolId,
            @PathVariable UUID quizId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) UUID studentId) {
        UUID effectiveStudentId = resolveStudentId(schoolId, studentId);
        if (effectiveStudentId == null && body != null && body.get("studentId") != null) {
            effectiveStudentId = UUID.fromString(body.get("studentId"));
        }
        if (effectiveStudentId == null) {
            throw new IllegalArgumentException("studentId is required");
        }
        return ResponseEntity.ok(quizService.startQuiz(quizId, effectiveStudentId));
    }

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizResultDto> submitQuiz(
            @PathVariable UUID schoolId,
            @PathVariable UUID quizId,
            @RequestBody(required = false) SubmitQuizRequest request,
            @RequestParam(required = false) UUID studentId) {
        UUID effectiveStudentId = resolveStudentId(schoolId, studentId);
        if (effectiveStudentId == null && request != null && request.getStudentId() != null) {
            effectiveStudentId = request.getStudentId();
        }
        if (effectiveStudentId == null) {
            throw new IllegalArgumentException("studentId is required");
        }
        return ResponseEntity.ok(quizService.submitQuiz(quizId, effectiveStudentId, request));
    }

    @GetMapping("/{quizId}/submissions")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<com.schoolsaas.model.QuizSubmission>> getSubmissions(
            @PathVariable UUID schoolId, @PathVariable UUID quizId) {
        return ResponseEntity.ok(quizService.getQuizSubmissions(quizId));
    }

    @GetMapping("/{quizId}/participants")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<com.schoolsaas.dto.quiz.QuizParticipantDto>> getParticipants(
            @PathVariable UUID schoolId,
            @PathVariable UUID quizId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) java.math.BigDecimal minScore,
            @RequestParam(required = false) java.math.BigDecimal maxScore) {
        return ResponseEntity.ok(quizService.getQuizParticipants(quizId, search, status, minScore, maxScore));
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

    @PostMapping("/{quizId}/release-results")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<QuizDto> releaseResults(
            @PathVariable UUID schoolId, @PathVariable UUID quizId) {
        return ResponseEntity.ok(quizService.releaseQuizResults(schoolId, quizId));
    }

    @PostMapping("/{quizId}/add-to-grades")
    @PreAuthorize("hasPermission(#schoolId, 'cms.content.edit') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> addToGrades(
            @PathVariable UUID schoolId, @PathVariable UUID quizId) {
        quizService.addQuizScoreToGrade(schoolId, quizId);
        return ResponseEntity.ok().build();
    }

    /**
     * Resolves the student ID from the explicit parameter or infers it from the
     * currently authenticated user if the user is a student in the given school.
     */
    private UUID resolveStudentId(UUID schoolId, UUID explicitStudentId) {
        if (explicitStudentId != null) {
            return explicitStudentId;
        }
        Optional<com.schoolsaas.security.UserPrincipal> userOpt = SecurityUtils.getCurrentUser();
        if (userOpt.isEmpty()) {
            return null;
        }
        return studentRepository.findByUserId(userOpt.get().getId())
                .filter(s -> s.getSchoolId().equals(schoolId))
                .map(com.schoolsaas.model.Student::getId)
                .orElse(null);
    }
}
