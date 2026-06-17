package com.schoolsaas.controller;

import com.schoolsaas.dto.grade.GradeResponse;
import com.schoolsaas.dto.parent.ParentDto;
import com.schoolsaas.dto.parent.ParentStudentInfo;
import com.schoolsaas.dto.subject.SubjectResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Parent;
import com.schoolsaas.model.ParentStudent;
import com.schoolsaas.model.StudentSubjectEnrollment;
import com.schoolsaas.repository.ParentRepository;
import com.schoolsaas.repository.ParentStudentRepository;
import com.schoolsaas.repository.StudentSubjectEnrollmentRepository;
import com.schoolsaas.repository.SubjectRepository;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.GradeService;
import com.schoolsaas.service.ParentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schools/{schoolId}/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;
    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final GradeService gradeService;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;

    @PostMapping
    public ResponseEntity<ParentDto> createParent(@PathVariable UUID schoolId, @RequestBody ParentDto dto) {
        return ResponseEntity.ok(parentService.createParent(schoolId, dto));
    }

    @GetMapping
    public ResponseEntity<Page<ParentDto>> listParents(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(parentService.listParents(schoolId, pageable));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<Page<ParentDto>> getParentsByStudent(@PathVariable UUID studentId, Pageable pageable) {
        List<ParentDto> list = parentService.getParentsByStudent(studentId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/me/children")
    public ResponseEntity<List<ParentStudentInfo>> getMyChildren(@PathVariable UUID schoolId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Parent parent = parentRepository.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", userId));
        return ResponseEntity.ok(parentService.getStudentChildren(parent.getId()));
    }

    @GetMapping("/me/children/{studentId}/grades")
    public ResponseEntity<List<GradeResponse>> getChildGrades(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Parent parent = parentRepository.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", userId));
        List<UUID> childIds = parentStudentRepository.findByParentId(parent.getId())
                .stream().map(ParentStudent::getStudentId).toList();
        if (!childIds.contains(studentId)) {
            throw new BadRequestException("This student is not linked to your account");
        }
        return ResponseEntity.ok(gradeService.getStudentGrades(schoolId, studentId));
    }

    @GetMapping("/me/children/{studentId}/subjects")
    public ResponseEntity<List<SubjectResponse>> getChildSubjects(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Parent parent = parentRepository.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", userId));
        List<UUID> childIds = parentStudentRepository.findByParentId(parent.getId())
                .stream().map(ParentStudent::getStudentId).toList();
        if (!childIds.contains(studentId)) {
            throw new BadRequestException("This student is not linked to your account");
        }
        List<StudentSubjectEnrollment> enrollments = enrollmentRepository.findBySchoolIdAndStudentId(schoolId, studentId);
        List<SubjectResponse> subjects = enrollments.stream()
                .map(e -> subjectRepository.findById(e.getSubjectId()).orElse(null))
                .filter(s -> s != null)
                .map(SubjectResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(subjects);
    }
}
