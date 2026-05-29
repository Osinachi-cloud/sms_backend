package com.schoolsaas.controller;

import com.schoolsaas.dto.teacher.CreateTeacherRequest;
import com.schoolsaas.dto.teacher.TeacherResponse;
import com.schoolsaas.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'teacher.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<TeacherResponse>> getTeachers(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(teacherService.getTeachers(schoolId, status, search, pageable));
    }

    @GetMapping("/{teacherId}")
    @PreAuthorize("hasPermission(#schoolId, 'teacher.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<TeacherResponse> getTeacher(
            @PathVariable UUID schoolId,
            @PathVariable UUID teacherId) {
        return ResponseEntity.ok(teacherService.getTeacher(schoolId, teacherId));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'teacher.create')")
    public ResponseEntity<TeacherResponse> createTeacher(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateTeacherRequest request) {
        return ResponseEntity.ok(teacherService.createTeacher(schoolId, request));
    }

    @PutMapping("/{teacherId}")
    @PreAuthorize("hasPermission(#schoolId, 'teacher.update')")
    public ResponseEntity<TeacherResponse> updateTeacher(
            @PathVariable UUID schoolId,
            @PathVariable UUID teacherId,
            @Valid @RequestBody CreateTeacherRequest request) {
        return ResponseEntity.ok(teacherService.updateTeacher(schoolId, teacherId, request));
    }

    @DeleteMapping("/{teacherId}")
    @PreAuthorize("hasPermission(#schoolId, 'teacher.delete')")
    public ResponseEntity<Void> deleteTeacher(
            @PathVariable UUID schoolId,
            @PathVariable UUID teacherId) {
        teacherService.deleteTeacher(schoolId, teacherId);
        return ResponseEntity.ok().build();
    }
}
