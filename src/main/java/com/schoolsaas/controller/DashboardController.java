package com.schoolsaas.controller;

import com.schoolsaas.dto.dashboard.DashboardStats;
import com.schoolsaas.dto.dashboard.StudentDashboard;
import com.schoolsaas.dto.dashboard.TeacherDashboard;
import com.schoolsaas.model.Student;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.TeacherRepository;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasPermission(#schoolId, 'analytics.academic.view') or hasPermission(#schoolId, 'school.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<DashboardStats> getSchoolStats(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(dashboardService.getSchoolDashboardStats(schoolId));
    }

    @GetMapping("/student")
    @PreAuthorize("hasPermission(#schoolId, 'student.grades.read')")
    public ResponseEntity<StudentDashboard> getStudentDashboard(@PathVariable UUID schoolId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Optional<Student> student = studentRepository.findByUserId(userId);

        if (student.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dashboardService.getStudentDashboard(schoolId, student.get().getId()));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasPermission(#schoolId, 'student.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<StudentDashboard> getStudentDashboardById(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(dashboardService.getStudentDashboard(schoolId, studentId));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasPermission(#schoolId, 'teacher.read') or hasPermission(#schoolId, 'class.read')")
    public ResponseEntity<TeacherDashboard> getTeacherDashboard(@PathVariable UUID schoolId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Optional<Teacher> teacher = teacherRepository.findByUserId(userId);

        if (teacher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dashboardService.getTeacherDashboard(schoolId, teacher.get().getId()));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasPermission(#schoolId, 'teacher.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<TeacherDashboard> getTeacherDashboardById(
            @PathVariable UUID schoolId,
            @PathVariable UUID teacherId) {
        return ResponseEntity.ok(dashboardService.getTeacherDashboard(schoolId, teacherId));
    }
}
