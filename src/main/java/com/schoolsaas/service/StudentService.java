package com.schoolsaas.service;

import com.schoolsaas.dto.student.CreateStudentRequest;
import com.schoolsaas.dto.student.StudentResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.School;
import com.schoolsaas.model.Student;
import com.schoolsaas.model.User;
import com.schoolsaas.model.UserSchool;
import com.schoolsaas.model.Role;
import com.schoolsaas.repository.ClassRepository;
import com.schoolsaas.repository.RoleRepository;
import com.schoolsaas.repository.SchoolRepository;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.UserRepository;
import com.schoolsaas.repository.UserSchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final UserSchoolRepository userSchoolRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<StudentResponse> getStudents(UUID schoolId, String status, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return studentRepository.searchBySchoolId(schoolId, search, pageable)
                    .map(StudentResponse::fromEntity);
        }
        if (status != null && !status.isBlank()) {
            return studentRepository.findBySchoolIdAndStatus(schoolId, status, pageable)
                    .map(StudentResponse::fromEntity);
        }
        return studentRepository.findBySchoolId(schoolId, pageable)
                .map(StudentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(UUID schoolId, UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        return StudentResponse.fromEntity(student);
    }

    @Transactional
    public StudentResponse createStudent(UUID schoolId, CreateStudentRequest request) {
        if (request.getEmail() != null && studentRepository.existsBySchoolIdAndEmail(schoolId, request.getEmail())) {
            throw new BadRequestException("Student with this email already exists");
        }

        if (request.getClassId() != null && !classRepository.existsById(request.getClassId())) {
            throw new BadRequestException("Class not found");
        }

        String admissionNumber = request.getAdmissionNumber();
        if (admissionNumber == null || admissionNumber.isBlank()) {
            admissionNumber = generateAdmissionNumber(schoolId);
        } else if (studentRepository.existsBySchoolIdAndAdmissionNumber(schoolId, admissionNumber)) {
            throw new BadRequestException("Admission number already exists");
        }

        Student student = Student.builder()
                .schoolId(schoolId)
                .admissionNumber(admissionNumber)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .classId(request.getClassId())
                .parentName(request.getParentName())
                .parentEmail(request.getParentEmail())
                .parentPhone(request.getParentPhone())
                .admissionDate(LocalDate.now())
                .status("ACTIVE")
                .metadata(request.getMetadata() != null ? request.getMetadata() : new java.util.HashMap<>())
                .build();

        student = studentRepository.save(student);

        // Create user account if email and password are provided
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && request.getPassword() != null && !request.getPassword().isBlank()) {
            if (!userRepository.existsByEmail(request.getEmail())) {
                User user = User.builder()
                        .email(request.getEmail())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .fullName(request.getFullName())
                        .phone(request.getPhone())
                        .emailVerified(true)
                        .isActive(true)
                        .build();
                user = userRepository.save(user);

                student.setUserId(user.getId());
                studentRepository.save(student);

                Role studentRole = roleRepository.findBySchoolIdAndName(schoolId, "STUDENT")
                        .orElseThrow(() -> new BadRequestException("STUDENT role not found for school"));

                UserSchool userSchool = UserSchool.builder()
                        .userId(user.getId())
                        .schoolId(schoolId)
                        .roleId(studentRole.getId())
                        .isActive(true)
                        .joinedAt(LocalDateTime.now())
                        .build();
                userSchoolRepository.save(userSchool);

                log.info("User account created for student: {} in school {}", user.getId(), schoolId);
            }
        }

        log.info("Student created: {} in school {}", student.getId(), schoolId);
        return StudentResponse.fromEntity(student);
    }

    @Transactional
    public StudentResponse updateStudent(UUID schoolId, UUID studentId, CreateStudentRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        if (request.getEmail() != null && !request.getEmail().equals(student.getEmail())) {
            if (studentRepository.existsBySchoolIdAndEmail(schoolId, request.getEmail())) {
                throw new BadRequestException("Student with this email already exists");
            }
            student.setEmail(request.getEmail());
        }

        if (request.getClassId() != null && !classRepository.existsById(request.getClassId())) {
            throw new BadRequestException("Class not found");
        }

        student.setFullName(request.getFullName());
        student.setPhone(request.getPhone());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setAddress(request.getAddress());
        student.setClassId(request.getClassId());
        student.setParentName(request.getParentName());
        student.setParentEmail(request.getParentEmail());
        student.setParentPhone(request.getParentPhone());
        if (request.getMetadata() != null) {
            student.setMetadata(request.getMetadata());
        }

        student = studentRepository.save(student);
        log.info("Student updated: {}", studentId);
        return StudentResponse.fromEntity(student);
    }

    @Transactional
    public void deleteStudent(UUID schoolId, UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        student.setStatus("INACTIVE");
        studentRepository.save(student);
        log.info("Student deleted (soft): {}", studentId);
    }

    private String generateAdmissionNumber(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        String prefix = school.getCode() + "/" + Year.now().getValue() + "/";
        Integer maxSeq = studentRepository.findMaxAdmissionNumberSequence(schoolId, prefix);
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;

        return prefix + String.format("%04d", nextSeq);
    }
}
