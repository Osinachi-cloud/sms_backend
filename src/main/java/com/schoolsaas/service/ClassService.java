package com.schoolsaas.service;

import com.schoolsaas.dto.classroom.ClassRequest;
import com.schoolsaas.dto.classroom.ClassResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.SchoolClass;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.model.TeacherClass;
import com.schoolsaas.repository.ClassRepository;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.TeacherClassRepository;
import com.schoolsaas.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;

    @Transactional(readOnly = true)
    public Page<ClassResponse> getClasses(UUID schoolId, String search, Pageable pageable) {
        Page<SchoolClass> classes;
        if (search != null && !search.isBlank()) {
            classes = classRepository.searchBySchoolId(schoolId, search, pageable);
        } else {
            classes = classRepository.findActiveBySchoolId(schoolId, pageable);
        }
        return classes.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ClassResponse getClass(UUID schoolId, UUID classId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!schoolClass.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Class", "id", classId);
        }
        return mapToResponse(schoolClass);
    }

    @Transactional
    public ClassResponse createClass(UUID schoolId, ClassRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Class name is required");
        }

        String name = request.getName().trim();
        String section = request.getSection() != null ? request.getSection().trim() : null;

        // Check for existing class (active or inactive) with same name/section
        SchoolClass existing = classRepository.findBySchoolIdAndNameAndSection(schoolId, name, section).orElse(null);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getIsActive())) {
                throw new BadRequestException("Class with this name and section already exists");
            }
            // Reactivate the previously deleted class
            existing.setIsActive(true);
            existing.setGradeLevel(request.getGradeLevel());
            existing.setCapacity(request.getCapacity());
            existing = classRepository.save(existing);

            // Update class teacher if provided
            if (request.getClassTeacherId() != null) {
                updateClassTeacher(schoolId, existing.getId(), request.getClassTeacherId());
            }
            return mapToResponse(existing);
        }

        SchoolClass schoolClass = SchoolClass.builder()
                .schoolId(schoolId)
                .name(name)
                .gradeLevel(request.getGradeLevel())
                .section(section)
                .capacity(request.getCapacity())
                .isActive(true)
                .build();

        schoolClass = classRepository.save(schoolClass);

        // Assign class teacher if provided
        if (request.getClassTeacherId() != null) {
            assignClassTeacher(schoolId, schoolClass.getId(), request.getClassTeacherId());
        }

        return mapToResponse(schoolClass);
    }

    @Transactional
    public ClassResponse updateClass(UUID schoolId, UUID classId, ClassRequest request) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        if (!schoolClass.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Class", "id", classId);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            String newSection = request.getSection() != null ? request.getSection().trim() : schoolClass.getSection();
            if (!newName.equals(schoolClass.getName()) || !java.util.Objects.equals(newSection, schoolClass.getSection())) {
                SchoolClass conflict = classRepository.findBySchoolIdAndNameAndSection(schoolId, newName, newSection).orElse(null);
                if (conflict != null && !conflict.getId().equals(classId)) {
                    throw new BadRequestException("Class with this name and section already exists");
                }
            }
            schoolClass.setName(newName);
        }
        if (request.getSection() != null) {
            schoolClass.setSection(request.getSection().trim());
        }
        if (request.getGradeLevel() != null) {
            schoolClass.setGradeLevel(request.getGradeLevel());
        }
        if (request.getCapacity() != null) {
            schoolClass.setCapacity(request.getCapacity());
        }

        schoolClass = classRepository.save(schoolClass);

        // Update class teacher assignment if provided
        if (request.getClassTeacherId() != null) {
            updateClassTeacher(schoolId, schoolClass.getId(), request.getClassTeacherId());
        }

        return mapToResponse(schoolClass);
    }

    @Transactional
    public void deleteClass(UUID schoolId, UUID classId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        if (!schoolClass.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Class", "id", classId);
        }

        schoolClass.setIsActive(false);
        classRepository.save(schoolClass);
    }

    private ClassResponse mapToResponse(SchoolClass schoolClass) {
        long studentCount = studentRepository.countBySchoolIdAndClassId(schoolClass.getSchoolId(), schoolClass.getId());

        // Find class teacher
        UUID classTeacherId = null;
        String classTeacherName = null;
        List<TeacherClass> assignments = teacherClassRepository.findByClassId(schoolClass.getId());
        for (TeacherClass tc : assignments) {
            if (Boolean.TRUE.equals(tc.getIsClassTeacher())) {
                classTeacherId = tc.getTeacherId();
                if (tc.getTeacher() != null) {
                    classTeacherName = tc.getTeacher().getFullName();
                } else {
                    Teacher teacher = teacherRepository.findById(tc.getTeacherId()).orElse(null);
                    classTeacherName = teacher != null ? teacher.getFullName() : null;
                }
                break;
            }
        }

        return ClassResponse.builder()
                .id(schoolClass.getId())
                .name(schoolClass.getName())
                .gradeLevel(schoolClass.getGradeLevel())
                .section(schoolClass.getSection())
                .capacity(schoolClass.getCapacity())
                .studentCount((int) studentCount)
                .classTeacherId(classTeacherId)
                .classTeacherName(classTeacherName)
                .isActive(schoolClass.getIsActive())
                .createdAt(schoolClass.getCreatedAt())
                .build();
    }

    private void assignClassTeacher(UUID schoolId, UUID classId, UUID teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        if (teacher == null || !teacher.getSchoolId().equals(schoolId)) {
            return; // Silently ignore invalid teacher
        }

        TeacherClass assignment = TeacherClass.builder()
                .teacherId(teacherId)
                .classId(classId)
                .isClassTeacher(true)
                .build();
        teacherClassRepository.save(assignment);
    }

    private void updateClassTeacher(UUID schoolId, UUID classId, UUID newTeacherId) {
        // Remove existing class teacher assignment for this class
        List<TeacherClass> assignments = teacherClassRepository.findByClassId(classId);
        for (TeacherClass tc : assignments) {
            if (Boolean.TRUE.equals(tc.getIsClassTeacher())) {
                teacherClassRepository.delete(tc);
            }
        }

        // Assign new class teacher if valid
        if (newTeacherId != null) {
            assignClassTeacher(schoolId, classId, newTeacherId);
        }
    }
}
