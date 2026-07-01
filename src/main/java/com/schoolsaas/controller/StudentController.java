package com.schoolsaas.controller;

import com.schoolsaas.dto.student.CreateStudentRequest;
import com.schoolsaas.dto.student.StudentDetailResponse;
import com.schoolsaas.dto.student.StudentResponse;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Student;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.model.TeacherClass;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.TeacherClassRepository;
import com.schoolsaas.repository.TeacherRepository;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.security.UserPrincipal;
import com.schoolsaas.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schools/{schoolId}/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<StudentResponse>> getStudents(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID classId,
            Pageable pageable) {
        return ResponseEntity.ok(studentService.getStudents(schoolId, status, search, classId, pageable));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StudentDetailResponse> getStudent(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {

        UserPrincipal user = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));

        // Admins and users with student.update get full view
        if (user.isPlatformAdmin() || SecurityUtils.hasPermission("student.update")) {
            return ResponseEntity.ok(studentService.getStudentDetail(schoolId, studentId));
        }

        // Check if user is a teacher in this school
        Optional<Teacher> teacherOpt = teacherRepository.findBySchoolIdAndUserId(schoolId, user.getId());
        if (teacherOpt.isPresent()) {
            Teacher teacher = teacherOpt.get();
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

            if (!student.getSchoolId().equals(schoolId)) {
                throw new ResourceNotFoundException("Student", "id", studentId);
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
                return ResponseEntity.ok(studentService.getStudentDetail(schoolId, studentId));
            }

            // Subject teacher — limited view (basic info only, no parents/attendance)
            return ResponseEntity.ok(studentService.getStudentDetailForSubjectTeacher(schoolId, studentId));
        }

        // Default: full view for other authorized roles
        return ResponseEntity.ok(studentService.getStudentDetail(schoolId, studentId));
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

    @PostMapping("/{studentId}/photo")
    @PreAuthorize("hasPermission(#schoolId, 'student.update')")
    public ResponseEntity<Map<String, String>> uploadStudentPhoto(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        String safeName = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = timestamp + "_" + safeName;

        Path uploadPath = Paths.get("uploads", schoolId.toString(), "photos");
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        String fileUrl = "/uploads/" + schoolId + "/photos/" + fileName;

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        student.setPhotoUrl(fileUrl);
        studentRepository.save(student);

        return ResponseEntity.ok(Map.of(
            "url", fileUrl,
            "name", originalFilename,
            "fullUrl", fileUrl
        ));
    }
}
