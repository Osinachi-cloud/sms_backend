package com.schoolsaas.controller;

import com.schoolsaas.dto.grade.GradeResponse;
import com.schoolsaas.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping("/students/{studentId}/grades")
    @PreAuthorize("hasPermission(#schoolId, 'student.grades.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<GradeResponse>> getStudentGrades(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(gradeService.getStudentGrades(schoolId, studentId));
    }

    @GetMapping("/students/{studentId}/grades/term/{termId}")
    @PreAuthorize("hasPermission(#schoolId, 'student.grades.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<GradeResponse>> getStudentGradesByTerm(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @PathVariable UUID termId) {
        return ResponseEntity.ok(gradeService.getGradesByTerm(schoolId, studentId, termId));
    }
}
