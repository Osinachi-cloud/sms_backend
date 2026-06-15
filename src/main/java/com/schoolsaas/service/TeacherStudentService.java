package com.schoolsaas.service;

import com.schoolsaas.dto.parent.ParentDto;
import com.schoolsaas.dto.student.StudentResponse;
import com.schoolsaas.dto.student.StudentWithParentsResponse;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Parent;
import com.schoolsaas.model.Student;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.model.TeacherClass;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherStudentService {

    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final ParentRepository parentRepository;

    @Transactional(readOnly = true)
    public List<StudentResponse> getStudentsForTeacher(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));
        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }

        List<TeacherClass> assignments = teacherClassRepository.findByTeacherId(teacherId);
        Set<UUID> classTeacherClassIds = assignments.stream()
                .filter(tc -> Boolean.TRUE.equals(tc.getIsClassTeacher()))
                .map(TeacherClass::getClassId)
                .collect(Collectors.toSet());

        // If specific classId requested, filter to that class (and subject if provided)
        if (classId != null) {
            boolean hasAccess = assignments.stream()
                    .anyMatch(tc -> tc.getClassId().equals(classId)
                            && (subjectId == null || subjectId.equals(tc.getSubjectId())));
            if (!hasAccess) {
                return List.of();
            }
            return studentRepository.findBySchoolIdAndClassId(schoolId, classId).stream()
                    .filter(s -> "ACTIVE".equals(s.getStatus()))
                    .map(s -> {
                        StudentResponse dto = StudentResponse.fromEntity(s);
                        dto.setIsClassTeacher(classTeacherClassIds.contains(s.getClassId()));
                        return dto;
                    })
                    .collect(Collectors.toList());
        }

        // Return all students from all assigned classes, optionally filtered by subject
        List<UUID> assignedClassIds = assignments.stream()
                .filter(tc -> subjectId == null || subjectId.equals(tc.getSubjectId()))
                .map(TeacherClass::getClassId)
                .distinct()
                .collect(Collectors.toList());

        return assignedClassIds.stream()
                .flatMap(cid -> studentRepository.findBySchoolIdAndClassId(schoolId, cid).stream())
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .distinct()
                .map(s -> {
                    StudentResponse dto = StudentResponse.fromEntity(s);
                    dto.setIsClassTeacher(classTeacherClassIds.contains(s.getClassId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentWithParentsResponse> getStudentsWithParentsForTeacher(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));
        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }

        List<TeacherClass> assignments = teacherClassRepository.findByTeacherId(teacherId);

        List<UUID> assignedClassIds;
        if (classId != null) {
            boolean hasAccess = assignments.stream()
                    .anyMatch(tc -> tc.getClassId().equals(classId)
                            && (subjectId == null || subjectId.equals(tc.getSubjectId())));
            if (!hasAccess) {
                return List.of();
            }
            assignedClassIds = List.of(classId);
        } else {
            assignedClassIds = assignments.stream()
                    .filter(tc -> subjectId == null || subjectId.equals(tc.getSubjectId()))
                    .map(TeacherClass::getClassId)
                    .distinct()
                    .collect(Collectors.toList());
        }

        List<Student> students = assignedClassIds.stream()
                .flatMap(cid -> studentRepository.findBySchoolIdAndClassId(schoolId, cid).stream())
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .distinct()
                .collect(Collectors.toList());

        // Batch-fetch parents
        Map<UUID, List<ParentDto>> parentsByStudent = students.stream()
                .collect(Collectors.toMap(
                        Student::getId,
                        s -> {
                            List<UUID> parentIds = parentStudentRepository.findByStudentId(s.getId()).stream()
                                    .map(ps -> ps.getParentId())
                                    .collect(Collectors.toList());
                            return parentRepository.findAllById(parentIds).stream()
                                    .map(this::mapParentToDto)
                                    .collect(Collectors.toList());
                        }
                ));

        return students.stream()
                .map(s -> StudentWithParentsResponse.builder()
                        .id(s.getId())
                        .admissionNumber(s.getAdmissionNumber())
                        .fullName(s.getFullName())
                        .email(s.getEmail())
                        .phone(s.getPhone())
                        .dateOfBirth(s.getDateOfBirth())
                        .gender(s.getGender())
                        .address(s.getAddress())
                        .classId(s.getClassId())
                        .className(s.getSchoolClass() != null ? s.getSchoolClass().getName() : null)
                        .parentName(s.getParentName())
                        .parentEmail(s.getParentEmail())
                        .parentPhone(s.getParentPhone())
                        .admissionDate(s.getAdmissionDate())
                        .status(s.getStatus())
                        .metadata(s.getMetadata())
                        .createdAt(s.getCreatedAt())
                        .parents(parentsByStudent.getOrDefault(s.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());
    }

    private ParentDto mapParentToDto(Parent parent) {
        ParentDto dto = new ParentDto();
        dto.setId(parent.getId());
        dto.setUserId(parent.getUserId());
        dto.setFullName(parent.getFullName());
        dto.setEmail(parent.getEmail());
        dto.setPhone(parent.getPhone());
        dto.setAddress(parent.getAddress());
        dto.setOccupation(parent.getOccupation());
        dto.setRelationship(parent.getRelationship());
        dto.setIsActive(parent.getIsActive());
        return dto;
    }
}
