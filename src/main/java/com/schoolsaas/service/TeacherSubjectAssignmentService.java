package com.schoolsaas.service;

import com.schoolsaas.dto.teacher.TeacherSubjectAssignmentDto;
import com.schoolsaas.dto.teacher.TeacherSubjectAssignmentRequest;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.TeacherClass;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherSubjectAssignmentService {

    private final TeacherClassRepository teacherClassRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicSessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public List<TeacherSubjectAssignmentDto> getAssignmentsByClass(UUID schoolId, UUID classId) {
        return teacherClassRepository.findByClassId(classId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherSubjectAssignmentDto> getAssignmentsByTeacher(UUID schoolId, UUID teacherId) {
        return teacherClassRepository.findByTeacherId(teacherId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherSubjectAssignmentDto> getClassTeachersBySubject(UUID schoolId, UUID classId, UUID subjectId) {
        return teacherClassRepository.findByClassId(classId).stream()
                .filter(tc -> tc.getSubjectId() != null && tc.getSubjectId().equals(subjectId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeacherSubjectAssignmentDto assignTeacher(UUID schoolId, TeacherSubjectAssignmentRequest request) {
        if (request.getTeacherId() == null) throw new BadRequestException("Teacher ID is required");
        if (request.getClassId() == null) throw new BadRequestException("Class ID is required");

        var teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getTeacherId()));
        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Teacher", "id", request.getTeacherId());
        }

        var cls = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.getClassId()));
        if (!cls.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Class", "id", request.getClassId());
        }

        if (request.getSubjectId() != null) {
            var subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));
            if (!subject.getSchoolId().equals(schoolId)) {
                throw new ResourceNotFoundException("Subject", "id", request.getSubjectId());
            }
        }

        TeacherClass assignment = TeacherClass.builder()
                .teacherId(request.getTeacherId())
                .classId(request.getClassId())
                .subjectId(request.getSubjectId())
                .isClassTeacher(request.getIsClassTeacher() != null ? request.getIsClassTeacher() : false)
                .sessionId(request.getSessionId())
                .build();

        assignment = teacherClassRepository.save(assignment);
        return mapToDto(assignment);
    }

    @Transactional
    public void removeAssignment(UUID schoolId, UUID assignmentId) {
        TeacherClass assignment = teacherClassRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        var teacher = teacherRepository.findById(assignment.getTeacherId()).orElse(null);
        if (teacher == null || !teacher.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Assignment", "id", assignmentId);
        }
        teacherClassRepository.delete(assignment);
    }

    private TeacherSubjectAssignmentDto mapToDto(TeacherClass tc) {
        String teacherName = teacherRepository.findById(tc.getTeacherId())
                .map(t -> t.getFullName()).orElse("Unknown");
        String className = classRepository.findById(tc.getClassId())
                .map(c -> c.getName()).orElse("Unknown");
        String subjectName = tc.getSubjectId() != null
                ? subjectRepository.findById(tc.getSubjectId()).map(s -> s.getName()).orElse("Unknown")
                : null;
        String sessionName = tc.getSessionId() != null
                ? sessionRepository.findById(tc.getSessionId()).map(s -> s.getName()).orElse(null)
                : null;

        return TeacherSubjectAssignmentDto.builder()
                .id(tc.getId())
                .teacherId(tc.getTeacherId())
                .teacherName(teacherName)
                .classId(tc.getClassId())
                .className(className)
                .subjectId(tc.getSubjectId())
                .subjectName(subjectName)
                .isClassTeacher(tc.getIsClassTeacher())
                .sessionId(tc.getSessionId())
                .sessionName(sessionName)
                .build();
    }
}
