package com.schoolsaas.controller;

import com.schoolsaas.dto.parent.ParentDto;
import com.schoolsaas.dto.student.StudentResponse;
import com.schoolsaas.dto.student.StudentWithParentsResponse;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.repository.TeacherRepository;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.ParentService;
import com.schoolsaas.service.TeacherStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/teacher-students")
@RequiredArgsConstructor
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;
    private final ParentService parentService;
    private final TeacherRepository teacherRepository;

    @GetMapping("/teachers/{teacherId}")
    @PreAuthorize("hasPermission(#schoolId, 'student.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<StudentResponse>> getTeacherStudents(
            @PathVariable UUID schoolId,
            @PathVariable UUID teacherId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID subjectId,
            Pageable pageable) {
        List<StudentResponse> list = teacherStudentService.getStudentsForTeacher(schoolId, teacherId, classId, subjectId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasPermission(#schoolId, 'student.read')")
    public ResponseEntity<Page<StudentResponse>> getMyStudents(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID subjectId,
            Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Optional<Teacher> teacher = teacherRepository.findByUserId(userId);
        if (teacher.isEmpty()) {
            return ResponseEntity.ok(new PageImpl<>(List.of(), pageable, 0));
        }
        List<StudentResponse> list = teacherStudentService.getStudentsForTeacher(schoolId, teacher.get().getId(), classId, subjectId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/me/with-parents")
    @PreAuthorize("hasPermission(#schoolId, 'student.read')")
    public ResponseEntity<Page<StudentWithParentsResponse>> getMyStudentsWithParents(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID subjectId,
            Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Optional<Teacher> teacher = teacherRepository.findByUserId(userId);
        if (teacher.isEmpty()) {
            return ResponseEntity.ok(new PageImpl<>(List.of(), pageable, 0));
        }
        List<StudentWithParentsResponse> list = teacherStudentService.getStudentsWithParentsForTeacher(
                schoolId, teacher.get().getId(), classId, subjectId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/teachers/{teacherId}/with-parents")
    @PreAuthorize("hasPermission(#schoolId, 'student.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<StudentWithParentsResponse>> getTeacherStudentsWithParents(
            @PathVariable UUID schoolId,
            @PathVariable UUID teacherId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID subjectId,
            Pageable pageable) {
        List<StudentWithParentsResponse> list = teacherStudentService.getStudentsWithParentsForTeacher(schoolId, teacherId, classId, subjectId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/students/{studentId}/parents")
    @PreAuthorize("hasPermission(#schoolId, 'student.read')")
    public ResponseEntity<Page<ParentDto>> getStudentParents(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            Pageable pageable) {
        List<ParentDto> list = parentService.getParentsByStudent(studentId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }
}
