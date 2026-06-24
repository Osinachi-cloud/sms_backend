package com.schoolsaas.controller;

import com.schoolsaas.dto.assessment.*;
import com.schoolsaas.service.AssessmentService;
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
@RequestMapping("/api/schools/{schoolId}/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AssessmentDto> create(
            @PathVariable UUID schoolId,
            @RequestBody AssessmentDto dto) {
        return ResponseEntity.ok(assessmentService.createAssessment(schoolId, dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AssessmentDto>> list(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) UUID teacherId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(assessmentService.listAssessments(schoolId, teacherId, search, pageable));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasPermission(#schoolId, 'student.grades.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<AssessmentDto>> adminList(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID termId) {
        return ResponseEntity.ok(assessmentService.listAllForAdmin(schoolId, classId, subjectId, termId));
    }

    @GetMapping("/{assessmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AssessmentDto> get(
            @PathVariable UUID schoolId,
            @PathVariable UUID assessmentId) {
        return ResponseEntity.ok(assessmentService.getAssessment(assessmentId));
    }

    @PutMapping("/{assessmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AssessmentDto> update(
            @PathVariable UUID schoolId,
            @PathVariable UUID assessmentId,
            @RequestBody AssessmentDto dto) {
        return ResponseEntity.ok(assessmentService.updateAssessment(schoolId, assessmentId, dto));
    }

    @DeleteMapping("/{assessmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID schoolId,
            @PathVariable UUID assessmentId) {
        assessmentService.deleteAssessment(schoolId, assessmentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{assessmentId}/scores")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveScores(
            @PathVariable UUID schoolId,
            @PathVariable UUID assessmentId,
            @RequestBody SaveScoresRequest request) {
        assessmentService.saveScores(schoolId, assessmentId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{assessmentId}/scores")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AssessmentScoreDto>> getScores(
            @PathVariable UUID schoolId,
            @PathVariable UUID assessmentId) {
        return ResponseEntity.ok(assessmentService.getScores(assessmentId));
    }

    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AssessmentDto>> available(
            @PathVariable UUID schoolId,
            @RequestParam UUID classId,
            @RequestParam UUID subjectId,
            @RequestParam UUID termId) {
        return ResponseEntity.ok(assessmentService.getAvailableAssessments(schoolId, classId, subjectId, termId));
    }

    @PostMapping("/grading-scheme")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveGradingScheme(
            @PathVariable UUID schoolId,
            @RequestBody SaveGradingSchemeRequest request) {
        assessmentService.saveGradingScheme(schoolId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/grading-scheme")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GradingSchemeEntryDto>> getGradingScheme(
            @PathVariable UUID schoolId,
            @RequestParam UUID classId,
            @RequestParam UUID subjectId,
            @RequestParam UUID termId) {
        return ResponseEntity.ok(assessmentService.getGradingScheme(schoolId, classId, subjectId, termId));
    }

    @GetMapping("/graded-scores")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> computeGradedScores(
            @PathVariable UUID schoolId,
            @RequestParam UUID classId,
            @RequestParam UUID subjectId,
            @RequestParam UUID termId) {
        return ResponseEntity.ok(assessmentService.computeGradedScores(schoolId, classId, subjectId, termId));
    }
}
