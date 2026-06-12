package com.schoolsaas.service;

import com.schoolsaas.dto.teacher.CreateTeacherRequest;
import com.schoolsaas.dto.teacher.TeacherResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Role;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.model.User;
import com.schoolsaas.model.UserSchool;
import com.schoolsaas.repository.RoleRepository;
import com.schoolsaas.repository.TeacherRepository;
import com.schoolsaas.repository.UserRepository;
import com.schoolsaas.repository.UserSchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final UserSchoolRepository userSchoolRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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
        return teacherRepository.findActiveBySchoolId(schoolId, pageable)
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

        // Create user account if email is provided (default password if blank)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!userRepository.existsByEmail(request.getEmail())) {
                String actualPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                        ? request.getPassword()
                        : "Password@12";

                User user = User.builder()
                        .email(request.getEmail())
                        .passwordHash(passwordEncoder.encode(actualPassword))
                        .fullName(request.getFullName())
                        .phone(request.getPhone())
                        .emailVerified(true)
                        .isActive(true)
                        .build();
                user = userRepository.save(user);

                teacher.setUserId(user.getId());
                teacherRepository.save(teacher);

                Role teacherRole = roleRepository.findBySchoolIdAndName(schoolId, "TEACHER")
                        .orElseThrow(() -> new BadRequestException("TEACHER role not found for school"));

                UserSchool userSchool = UserSchool.builder()
                        .userId(user.getId())
                        .schoolId(schoolId)
                        .roleId(teacherRole.getId())
                        .isActive(true)
                        .joinedAt(LocalDateTime.now())
                        .build();
                userSchoolRepository.save(userSchool);

                log.info("User account created for teacher: {} in school {} with password [{}]",
                        user.getId(), schoolId,
                        (request.getPassword() != null && !request.getPassword().isBlank()) ? "provided" : "default");
            }
        }

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
