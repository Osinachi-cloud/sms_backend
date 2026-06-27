package com.schoolsaas.service;

import com.schoolsaas.dto.subject.SubjectRequest;
import com.schoolsaas.dto.subject.SubjectResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.GradingScheme;
import com.schoolsaas.model.Student;
import com.schoolsaas.model.StudentSubjectEnrollment;
import com.schoolsaas.model.Subject;
import com.schoolsaas.repository.ClassRepository;
import com.schoolsaas.repository.GradingSchemeRepository;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.StudentSubjectEnrollmentRepository;
import com.schoolsaas.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final GradingSchemeRepository gradingSchemeRepository;

    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjects(UUID schoolId) {
        return subjectRepository.findBySchoolIdAndIsActiveTrue(schoolId)
                .stream()
                .map(s -> mapToResponse(s, schoolId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsForStudent(UUID schoolId, UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }
        UUID classId = student.getClassId();
        List<Subject> subjects = subjectRepository.findBySchoolIdAndIsActiveTrue(schoolId);
        if (classId != null) {
            subjects = subjects.stream()
                    .filter(s -> s.getClassIds() != null && s.getClassIds().contains(classId))
                    .collect(Collectors.toList());
        }
        return subjects.stream()
                .map(s -> mapToResponse(s, schoolId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubject(UUID schoolId, UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
        if (!subject.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Subject", "id", subjectId);
        }
        return mapToResponse(subject, schoolId);
    }

    @Transactional
    public SubjectResponse createSubject(UUID schoolId, SubjectRequest request, UUID createdBy, String createdByType) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Subject name is required");
        }
        // Validate grading scheme: a subject MUST have a grading scheme assigned
        UUID schemeId = request.getGradingSchemeId();
        if (schemeId == null) {
            // Fall back to the default scheme for this school if none provided
            schemeId = gradingSchemeRepository.findBySchoolIdAndIsDefaultTrue(schoolId)
                    .map(gs -> gs.getId())
                    .orElse(null);
        }
        if (schemeId == null) {
            throw new BadRequestException(
                "No grading scheme is configured for this school. " +
                "Please go to Settings > Grading Schemes and create a default scheme first, then try again."
            );
        }
        // Verify the scheme exists and belongs to this school
        GradingScheme scheme = gradingSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new BadRequestException("Selected grading scheme does not exist."));
        if (!scheme.getSchoolId().equals(schoolId)) {
            throw new BadRequestException("Selected grading scheme does not belong to this school.");
        }

        // Teacher-created subjects are automatically free unless admin overrides
        boolean isFree = request.getIsFree() != null ? request.getIsFree() : (!"TEACHER".equals(createdByType));
        BigDecimal cost = request.getCost() != null ? request.getCost() : BigDecimal.ZERO;
        List<UUID> classIds = request.getClassIds() != null ? request.getClassIds() : List.of();

        Subject subject = Subject.builder()
                .schoolId(schoolId)
                .name(request.getName().trim())
                .code(request.getCode() != null ? request.getCode().trim() : null)
                .description(request.getDescription())
                .isActive(true)
                .isFree(isFree)
                .cost(cost)
                .createdBy(createdBy)
                .createdByType(createdByType)
                .classIds(classIds)
                .gradingSchemeId(schemeId)
                .build();
        subject = subjectRepository.save(subject);

        // Auto-enroll all students in linked classes if the subject is free
        if (Boolean.TRUE.equals(isFree) && !classIds.isEmpty()) {
            autoEnrollStudents(schoolId, subject.getId(), classIds);
        }

        return mapToResponse(subject, schoolId);
    }

    @Transactional
    public SubjectResponse updateSubject(UUID schoolId, UUID subjectId, SubjectRequest request, boolean isAdmin) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
        if (!subject.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Subject", "id", subjectId);
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            subject.setName(request.getName().trim());
        }
        if (request.getCode() != null) {
            subject.setCode(request.getCode().trim());
        }
        if (request.getDescription() != null) {
            subject.setDescription(request.getDescription());
        }
        // Admins can override free/paid status for any subject including teacher-created ones
        if (request.getIsFree() != null && isAdmin) {
            subject.setIsFree(request.getIsFree());
        }
        if (request.getCost() != null && isAdmin) {
            subject.setCost(request.getCost());
        }
        List<UUID> oldClassIds = subject.getClassIds() != null ? subject.getClassIds() : List.of();
        if (request.getClassIds() != null) {
            subject.setClassIds(request.getClassIds());
        }
        // Allow admins to assign/unassign grading scheme with validation
        if (request.getGradingSchemeId() != null) {
            GradingScheme scheme = gradingSchemeRepository.findById(request.getGradingSchemeId())
                    .orElseThrow(() -> new BadRequestException("Selected grading scheme does not exist."));
            if (!scheme.getSchoolId().equals(schoolId)) {
                throw new BadRequestException("Selected grading scheme does not belong to this school.");
            }
            subject.setGradingSchemeId(request.getGradingSchemeId());
        }
        subject = subjectRepository.save(subject);

        // Auto-enroll newly added class students if free
        if (Boolean.TRUE.equals(subject.getIsFree()) && request.getClassIds() != null) {
            List<UUID> newClassIds = request.getClassIds().stream()
                    .filter(cid -> !oldClassIds.contains(cid))
                    .collect(Collectors.toList());
            if (!newClassIds.isEmpty()) {
                autoEnrollStudents(schoolId, subject.getId(), newClassIds);
            }
        }

        return mapToResponse(subject, schoolId);
    }

    @Transactional
    public void deleteSubject(UUID schoolId, UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
        if (!subject.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Subject", "id", subjectId);
        }
        subject.setIsActive(false);
        subjectRepository.save(subject);
    }

    private void autoEnrollStudents(UUID schoolId, UUID subjectId, List<UUID> classIds) {
        for (UUID classId : classIds) {
            List<Student> students = studentRepository.findActiveBySchoolIdAndClassId(schoolId, classId);
            for (Student student : students) {
                if (!enrollmentRepository.existsBySchoolIdAndStudentIdAndSubjectId(schoolId, student.getId(), subjectId)) {
                    enrollmentRepository.save(StudentSubjectEnrollment.builder()
                            .schoolId(schoolId)
                            .studentId(student.getId())
                            .subjectId(subjectId)
                            .status("ENROLLED")
                            .build());
                }
            }
        }
        log.info("Auto-enrolled students for subject {} in classes {}", subjectId, classIds);
    }

    private SubjectResponse mapToResponse(Subject subject, UUID schoolId) {
        long enrollmentCount = enrollmentRepository.countBySchoolIdAndSubjectIdAndStatus(schoolId, subject.getId(), "ENROLLED");
        List<String> classNames = List.of();
        if (subject.getClassIds() != null && !subject.getClassIds().isEmpty()) {
            classNames = classRepository.findAllById(subject.getClassIds()).stream()
                    .map(c -> c.getName())
                    .collect(Collectors.toList());
        }
        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .isActive(subject.getIsActive())
                .isFree(subject.getIsFree())
                .cost(subject.getCost())
                .createdBy(subject.getCreatedBy())
                .createdByType(subject.getCreatedByType())
                .classIds(subject.getClassIds())
                .classNames(classNames)
                .enrollmentCount(enrollmentCount)
                .gradingSchemeId(subject.getGradingSchemeId())
                .createdAt(subject.getCreatedAt())
                .build();
    }
}
