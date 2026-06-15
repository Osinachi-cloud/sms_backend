package com.schoolsaas.controller;

import com.schoolsaas.dto.teacher.TeacherSubjectAssignmentDto;
import com.schoolsaas.dto.teacher.TeacherSubjectAssignmentRequest;
import com.schoolsaas.service.TeacherSubjectAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/teacher-assignments")
@RequiredArgsConstructor
public class TeacherSubjectAssignmentController {

    private final TeacherSubjectAssignmentService assignmentService;

    @GetMapping("/classes/{classId}")
    @PreAuthorize("hasPermission(#schoolId, 'class.read') or hasPermission(#schoolId, 'teacher.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<TeacherSubjectAssignmentDto>> getAssignmentsByClass(@PathVariable UUID schoolId, @PathVariable UUID classId, Pageable pageable) {
        List<TeacherSubjectAssignmentDto> list = assignmentService.getAssignmentsByClass(schoolId, classId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/teachers/{teacherId}")
    @PreAuthorize("hasPermission(#schoolId, 'teacher.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<TeacherSubjectAssignmentDto>> getAssignmentsByTeacher(@PathVariable UUID schoolId, @PathVariable UUID teacherId, Pageable pageable) {
        List<TeacherSubjectAssignmentDto> list = assignmentService.getAssignmentsByTeacher(schoolId, teacherId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'teacher.assign.class') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<TeacherSubjectAssignmentDto> assignTeacher(
            @PathVariable UUID schoolId,
            @RequestBody TeacherSubjectAssignmentRequest request) {
        return ResponseEntity.ok(assignmentService.assignTeacher(schoolId, request));
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasPermission(#schoolId, 'teacher.assign.class') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> removeAssignment(@PathVariable UUID schoolId, @PathVariable UUID assignmentId) {
        assignmentService.removeAssignment(schoolId, assignmentId);
        return ResponseEntity.ok().build();
    }
}
