package com.schoolsaas.service;

import com.schoolsaas.dto.teacher.CreateTeacherRequest;
import com.schoolsaas.dto.teacher.TeacherResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public Page<TeacherResponse> getTeachers(UUID schoolId, String status, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return teacherRepository.searchBySchoolId(schoolId, search, pageable)
                    .map(TeacherResponse::fromEntity);
        }
        if (status != null && !status.isBlank()) {
            return teacherRepository.findBySchoolIdAndStatus(schoolId, status, pageable)
                    .map(TeacherResponse::fromEntity);
        }
        return teacherRepository.findBySchoolId(schoolId, pageable)
                .map(TeacherResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacher(UUID schoolId, UUID teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }

        return TeacherResponse.fromEntity(teacher);
    }

    @Transactional
    public TeacherResponse createTeacher(UUID schoolId, CreateTeacherRequest request) {
        if (request.getEmail() != null && teacherRepository.existsBySchoolIdAndEmail(schoolId, request.getEmail())) {
            throw new BadRequestException("Teacher with this email already exists");
        }

        if (request.getEmployeeId() != null && teacherRepository.existsBySchoolIdAndEmployeeId(schoolId, request.getEmployeeId())) {
            throw new BadRequestException("Employee ID already exists");
        }

        Teacher teacher = Teacher.builder()
                .schoolId(schoolId)
                .employeeId(request.getEmployeeId())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .specialization(request.getSpecialization())
                .qualification(request.getQualification())
                .dateOfJoining(request.getDateOfJoining())
                .status("ACTIVE")
                .metadata(request.getMetadata() != null ? request.getMetadata() : new java.util.HashMap<>())
                .build();

        teacher = teacherRepository.save(teacher);
        log.info("Teacher created: {} in school {}", teacher.getId(), schoolId);
        return TeacherResponse.fromEntity(teacher);
    }

    @Transactional
    public TeacherResponse updateTeacher(UUID schoolId, UUID teacherId, CreateTeacherRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }

        if (request.getEmail() != null && !request.getEmail().equals(teacher.getEmail())) {
            if (teacherRepository.existsBySchoolIdAndEmail(schoolId, request.getEmail())) {
                throw new BadRequestException("Teacher with this email already exists");
            }
            teacher.setEmail(request.getEmail());
        }

        if (request.getEmployeeId() != null && !request.getEmployeeId().equals(teacher.getEmployeeId())) {
            if (teacherRepository.existsBySchoolIdAndEmployeeId(schoolId, request.getEmployeeId())) {
                throw new BadRequestException("Employee ID already exists");
            }
            teacher.setEmployeeId(request.getEmployeeId());
        }

        teacher.setFullName(request.getFullName());
        teacher.setPhone(request.getPhone());
        teacher.setSpecialization(request.getSpecialization());
        teacher.setQualification(request.getQualification());
        teacher.setDateOfJoining(request.getDateOfJoining());
        if (request.getMetadata() != null) {
            teacher.setMetadata(request.getMetadata());
        }

        teacher = teacherRepository.save(teacher);
        log.info("Teacher updated: {}", teacherId);
        return TeacherResponse.fromEntity(teacher);
    }

    @Transactional
    public void deleteTeacher(UUID schoolId, UUID teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }

        teacher.setStatus("INACTIVE");
        teacherRepository.save(teacher);
        log.info("Teacher deleted (soft): {}", teacherId);
    }
}
