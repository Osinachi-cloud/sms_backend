package com.schoolsaas.service;

import com.schoolsaas.dto.classroom.ClassRequest;
import com.schoolsaas.dto.classroom.ClassResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.SchoolClass;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.repository.ClassRepository;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

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

        SchoolClass schoolClass = SchoolClass.builder()
                .schoolId(schoolId)
                .name(request.getName().trim())
                .gradeLevel(request.getGradeLevel())
                .section(request.getSection() != null ? request.getSection().trim() : null)
                .capacity(request.getCapacity())
                .isActive(true)
                .build();

        schoolClass = classRepository.save(schoolClass);
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
            schoolClass.setName(request.getName().trim());
        }
        if (request.getGradeLevel() != null) {
            schoolClass.setGradeLevel(request.getGradeLevel());
        }
        if (request.getSection() != null) {
            schoolClass.setSection(request.getSection().trim());
        }
        if (request.getCapacity() != null) {
            schoolClass.setCapacity(request.getCapacity());
        }

        schoolClass = classRepository.save(schoolClass);
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

        return ClassResponse.builder()
                .id(schoolClass.getId())
                .name(schoolClass.getName())
                .gradeLevel(schoolClass.getGradeLevel())
                .section(schoolClass.getSection())
                .capacity(schoolClass.getCapacity())
                .studentCount((int) studentCount)
                .isActive(schoolClass.getIsActive())
                .createdAt(schoolClass.getCreatedAt())
                .build();
    }
}
