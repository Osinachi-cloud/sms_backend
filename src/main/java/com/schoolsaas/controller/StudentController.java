package com.schoolsaas.controller;

import com.schoolsaas.dto.student.CreateStudentRequest;
import com.schoolsaas.dto.student.StudentResponse;
import com.schoolsaas.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'student.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<StudentResponse>> getStudents(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(studentService.getStudents(schoolId, status, search, pageable));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasPermission(#schoolId, 'student.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<StudentResponse> getStudent(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(studentService.getStudent(schoolId, studentId));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'student.create')")
    public ResponseEntity<StudentResponse> createStudent(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.ok(studentService.createStudent(schoolId, request));
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasPermission(#schoolId, 'student.update')")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(schoolId, studentId, request));
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasPermission(#schoolId, 'student.delete')")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        studentService.deleteStudent(schoolId, studentId);
        return ResponseEntity.ok().build();
    }
}
