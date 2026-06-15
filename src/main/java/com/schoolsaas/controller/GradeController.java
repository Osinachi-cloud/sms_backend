package com.schoolsaas.controller;

import com.schoolsaas.dto.grade.GradeResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Student;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.model.TeacherClass;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.TeacherClassRepository;
import com.schoolsaas.repository.TeacherRepository;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.security.UserPrincipal;
import com.schoolsaas.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schools/{schoolId}")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final StudentRepository studentRepository;

    @GetMapping("/students/{studentId}/grades")
    @PreAuthorize("hasPermission(#schoolId, 'student.grades.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<GradeResponse>> getStudentGrades(
            @PathVariable UUID schoolId,
            @PathVariable String studentId) {

        UserPrincipal user = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));

        UUID resolvedStudentId = resolveStudentId(schoolId, studentId, user);

        // Admins get all grades
        if (user.isPlatformAdmin() || SecurityUtils.hasPermission("student.update")) {
            return ResponseEntity.ok(gradeService.getStudentGrades(schoolId, resolvedStudentId));
        }

        // Check if user is a teacher in this school
        Optional<Teacher> teacherOpt = teacherRepository.findBySchoolIdAndUserId(schoolId, user.getId());
        if (teacherOpt.isPresent()) {
            Teacher teacher = teacherOpt.get();
            Student student = studentRepository.findById(resolvedStudentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", resolvedStudentId));

            if (!student.getSchoolId().equals(schoolId)) {
                throw new ResourceNotFoundException("Student", "id", resolvedStudentId);
            }

            List<TeacherClass> assignments = teacherClassRepository.findByTeacherId(teacher.getId());
            List<TeacherClass> classAssignments = assignments.stream()
                    .filter(tc -> tc.getClassId().equals(student.getClassId()))
                    .collect(Collectors.toList());

            if (classAssignments.isEmpty()) {
                return ResponseEntity.status(403).build();
            }

            boolean isClassTeacher = classAssignments.stream()
                    .anyMatch(tc -> Boolean.TRUE.equals(tc.getIsClassTeacher()));

            if (isClassTeacher) {
                return ResponseEntity.ok(gradeService.getStudentGrades(schoolId, resolvedStudentId));
            }

            // Subject teacher — filter grades by their subjects
            List<UUID> subjectIds = classAssignments.stream()
                    .filter(tc -> tc.getSubjectId() != null)
                    .map(TeacherClass::getSubjectId)
                    .distinct()
                    .collect(Collectors.toList());

            return ResponseEntity.ok(gradeService.getStudentGrades(schoolId, resolvedStudentId, subjectIds));
        }

        // Default: all grades for other authorized roles (parents, students, etc.)
        return ResponseEntity.ok(gradeService.getStudentGrades(schoolId, resolvedStudentId));
    }

    @GetMapping("/students/{studentId}/grades/term/{termId}")
    @PreAuthorize("hasPermission(#schoolId, 'student.grades.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<GradeResponse>> getStudentGradesByTerm(
            @PathVariable UUID schoolId,
            @PathVariable String studentId,
            @PathVariable UUID termId) {

        UserPrincipal user = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));

        UUID resolvedStudentId = resolveStudentId(schoolId, studentId, user);

        List<GradeResponse> grades = gradeService.getGradesByTerm(schoolId, resolvedStudentId, termId);

        // Check if user is a subject teacher (not class teacher)
        Optional<Teacher> teacherOpt = teacherRepository.findBySchoolIdAndUserId(schoolId, user.getId());
        if (teacherOpt.isPresent()) {
            Teacher teacher = teacherOpt.get();
            Student student = studentRepository.findById(resolvedStudentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", resolvedStudentId));

            List<TeacherClass> assignments = teacherClassRepository.findByTeacherId(teacher.getId());
            List<TeacherClass> classAssignments = assignments.stream()
                    .filter(tc -> tc.getClassId().equals(student.getClassId()))
                    .collect(Collectors.toList());

            boolean isClassTeacher = classAssignments.stream()
                    .anyMatch(tc -> Boolean.TRUE.equals(tc.getIsClassTeacher()));

            if (!isClassTeacher && !classAssignments.isEmpty()) {
                List<UUID> subjectIds = classAssignments.stream()
                        .filter(tc -> tc.getSubjectId() != null)
                        .map(TeacherClass::getSubjectId)
                        .distinct()
                        .collect(Collectors.toList());

                grades = grades.stream()
                        .filter(g -> subjectIds.contains(g.getSubjectId()))
                        .collect(Collectors.toList());
            }
        }

        return ResponseEntity.ok(grades);
    }

    private UUID resolveStudentId(UUID schoolId, String studentId, UserPrincipal user) {
        if ("current".equalsIgnoreCase(studentId)) {
            return studentRepository.findByUserId(user.getId())
                    .filter(s -> s.getSchoolId().equals(schoolId))
                    .map(Student::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", user.getId()));
        }
        try {
            return UUID.fromString(studentId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid student ID: " + studentId);
        }
    }
}
