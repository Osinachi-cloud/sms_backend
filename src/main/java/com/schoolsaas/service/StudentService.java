package com.schoolsaas.service;

import com.schoolsaas.dto.student.CreateStudentRequest;
import com.schoolsaas.dto.student.StudentResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.ClassRepository;
import com.schoolsaas.repository.ParentRepository;
import com.schoolsaas.repository.ParentStudentRepository;
import com.schoolsaas.repository.PaymentRepository;
import com.schoolsaas.repository.RoleRepository;
import com.schoolsaas.repository.SchoolRepository;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.TeacherClassRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final PaymentRepository paymentRepository;
    private final AttendanceService attendanceService;
    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "Password@12";

    private String generateSyntheticEmail(String prefix, UUID id) {
        return prefix + "-" + id + "@placeholder.local";
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> getStudents(UUID schoolId, String status, String search, UUID classId, Pageable pageable) {
        // If classId is provided, scope to that class
        if (classId != null) {
            if (search != null && !search.isBlank()) {
                return studentRepository.searchBySchoolIdAndClassId(schoolId, classId, search, pageable)
                        .map(StudentResponse::fromEntity);
            }
            if (status != null && !status.isBlank()) {
                return studentRepository.findBySchoolIdAndClassIdAndStatus(schoolId, classId, status, pageable)
                        .map(StudentResponse::fromEntity);
            }
            return studentRepository.findActiveBySchoolIdAndClassId(schoolId, classId, pageable)
                    .map(StudentResponse::fromEntity);
        }

        // No class filter — return all (admin view)
        if (search != null && !search.isBlank()) {
            return studentRepository.searchBySchoolId(schoolId, search, pageable)
                    .map(StudentResponse::fromEntity);
        }
        if (status != null && !status.isBlank()) {
            return studentRepository.findBySchoolIdAndStatus(schoolId, status, pageable)
                    .map(StudentResponse::fromEntity);
        }
        return studentRepository.findActiveBySchoolId(schoolId, pageable)
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

    @Transactional(readOnly = true)
    public com.schoolsaas.dto.student.StudentDetailResponse getStudentDetail(UUID schoolId, UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        // Get linked parents
        List<ParentStudent> parentLinks = parentStudentRepository.findByStudentId(studentId);
        List<UUID> parentIds = parentLinks.stream()
                .map(com.schoolsaas.model.ParentStudent::getParentId)
                .collect(Collectors.toList());
        List<com.schoolsaas.model.Parent> parents = parentRepository.findAllById(parentIds);

        List<com.schoolsaas.dto.student.StudentDetailResponse.ParentInfo> parentInfos = parents.stream()
                .map(p -> {
                    boolean isPrimary = parentLinks.stream()
                            .anyMatch(link -> link.getParentId().equals(p.getId()) && Boolean.TRUE.equals(link.getIsPrimary()));
                    return com.schoolsaas.dto.student.StudentDetailResponse.ParentInfo.builder()
                            .id(p.getId())
                            .fullName(p.getFullName())
                            .email(p.getEmail())
                            .phone(p.getPhone())
                            .relationship(p.getRelationship())
                            .address(p.getAddress())
                            .occupation(p.getOccupation())
                            .isPrimary(isPrimary)
                            .build();
                })
                .collect(Collectors.toList());

        // Get attendance summary
        com.schoolsaas.dto.attendance.AttendanceSummary attendanceSummary = attendanceService.getStudentAttendanceSummary(schoolId, studentId);

        return com.schoolsaas.dto.student.StudentDetailResponse.builder()
                .id(student.getId())
                .admissionNumber(student.getAdmissionNumber())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .classId(student.getClassId())
                .className(student.getSchoolClass() != null ? student.getSchoolClass().getName() : null)
                .admissionDate(student.getAdmissionDate())
                .status(student.getStatus())
                .metadata(student.getMetadata())
                .createdAt(student.getCreatedAt())
                .parentName(student.getParentName())
                .parentEmail(student.getParentEmail())
                .parentPhone(student.getParentPhone())
                .parents(parentInfos)
                .attendanceSummary(attendanceSummary)
                .limitedView(false)
                .build();
    }

    @Transactional(readOnly = true)
    public com.schoolsaas.dto.student.StudentDetailResponse getStudentDetailForSubjectTeacher(UUID schoolId, UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        return com.schoolsaas.dto.student.StudentDetailResponse.builder()
                .id(student.getId())
                .admissionNumber(student.getAdmissionNumber())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .classId(student.getClassId())
                .className(student.getSchoolClass() != null ? student.getSchoolClass().getName() : null)
                .admissionDate(student.getAdmissionDate())
                .status(student.getStatus())
                .metadata(student.getMetadata())
                .createdAt(student.getCreatedAt())
                .parentName(null)
                .parentEmail(null)
                .parentPhone(null)
                .parents(List.of())
                .attendanceSummary(null)
                .limitedView(true)
                .build();
    }

    @Transactional
    public StudentResponse createStudent(UUID schoolId, CreateStudentRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("User with this email already exists");
            }
            if (studentRepository.existsBySchoolIdAndEmail(schoolId, request.getEmail())) {
                throw new BadRequestException("Student with this email already exists");
            }
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }

        if (request.getClassId() != null && !classRepository.existsById(request.getClassId())) {
            throw new BadRequestException("Class not found");
        }

        // Normalize and validate gender
        String gender = request.getGender();
        if (gender != null && !gender.isBlank()) {
            gender = gender.trim().toUpperCase();
            if (!gender.equals("MALE") && !gender.equals("FEMALE") && !gender.equals("OTHER")) {
                throw new BadRequestException("Invalid gender: " + gender + ". Must be MALE, FEMALE or OTHER.");
            }
        } else {
            gender = null;
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
                .gender(gender)
                .address(request.getAddress())
                .classId(request.getClassId())
                .parentName(request.getParentName())
                .parentEmail(request.getParentEmail())
                .parentPhone(request.getParentPhone())
                .photoUrl(request.getPhotoUrl())
                .admissionDate(LocalDate.now())
                .status("ACTIVE")
                .metadata(request.getMetadata() != null ? request.getMetadata() : new java.util.HashMap<>())
                .build();

        student = studentRepository.save(student);

        // Create or link user account for student
        User studentUser = null;
        String actualPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : DEFAULT_PASSWORD;

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            // Student uses username (no email required)
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BadRequestException("Username already taken");
            }
            studentUser = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail()) // may be null
                    .passwordHash(passwordEncoder.encode(actualPassword))
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .emailVerified(true)
                    .isActive(true)
                    .build();
            studentUser = userRepository.save(studentUser);
        } else if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Student uses email
            studentUser = User.builder()
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(actualPassword))
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .emailVerified(true)
                    .isActive(true)
                    .build();
            studentUser = userRepository.save(studentUser);
        } else {
            // Fallback: synthetic email for backward compatibility
            String syntheticEmail = generateSyntheticEmail("student", student.getId());
            if (userRepository.existsByEmail(syntheticEmail)) {
                studentUser = userRepository.findByEmail(syntheticEmail)
                        .orElseThrow(() -> new BadRequestException("User with email exists but could not be found"));
            } else {
                studentUser = User.builder()
                        .email(syntheticEmail)
                        .passwordHash(passwordEncoder.encode(actualPassword))
                        .fullName(request.getFullName())
                        .phone(request.getPhone())
                        .emailVerified(true)
                        .isActive(true)
                        .build();
                studentUser = userRepository.save(studentUser);
            }
        }

        student.setUserId(studentUser.getId());
        studentRepository.save(student);

        Role studentRole = roleRepository.findBySchoolIdAndName(schoolId, "STUDENT")
                .orElseThrow(() -> new BadRequestException("STUDENT role not found for school"));

        if (!userSchoolRepository.existsByUserIdAndSchoolId(studentUser.getId(), schoolId)) {
            UserSchool userSchool = UserSchool.builder()
                    .userId(studentUser.getId())
                    .schoolId(schoolId)
                    .roleId(studentRole.getId())
                    .isActive(true)
                    .joinedAt(LocalDateTime.now())
                    .build();
            userSchoolRepository.save(userSchool);
        }

        log.info("User account linked for student: {} in school {}", studentUser.getId(), schoolId);

        // Process parent: link existing or create new
        if (request.getParent() != null) {
            CreateStudentRequest.ParentPayload parentPayload = request.getParent();
            if (parentPayload.getParentId() != null) {
                // Link existing parent
                linkExistingParent(schoolId, student.getId(), parentPayload.getParentId());
                Parent existingParent = parentRepository.findById(parentPayload.getParentId()).orElse(null);
                if (existingParent != null) {
                    student.setParentName(existingParent.getFullName());
                    student.setParentEmail(existingParent.getEmail());
                    student.setParentPhone(existingParent.getPhone());
                    studentRepository.save(student);
                }
            } else if (parentPayload.getFullName() != null && !parentPayload.getFullName().isBlank()) {
                // Create new parent
                Parent newParent = createParentFromPayload(schoolId, parentPayload);
                if (newParent != null) {
                    linkExistingParent(schoolId, student.getId(), newParent.getId());
                    student.setParentName(newParent.getFullName());
                    student.setParentEmail(newParent.getEmail());
                    student.setParentPhone(newParent.getPhone());
                    studentRepository.save(student);
                }
            }
        }

        log.info("Student created: {} in school {}", student.getId(), schoolId);
        return StudentResponse.fromEntity(student);
    }

    private Parent createParentFromPayload(UUID schoolId, CreateStudentRequest.ParentPayload payload) {
        Parent parent = Parent.builder()
                .schoolId(schoolId)
                .fullName(payload.getFullName().trim())
                .email(payload.getEmail())
                .phone(payload.getPhone())
                .address(payload.getAddress())
                .occupation(payload.getOccupation())
                .relationship(payload.getRelationship() != null && !payload.getRelationship().isBlank()
                        ? payload.getRelationship().trim().toUpperCase()
                        : "GUARDIAN")
                .isActive(true)
                .build();

        parent = parentRepository.save(parent);

        // Create or link user account for parent (synthetic email fallback)
        String parentEmail = (payload.getEmail() != null && !payload.getEmail().isBlank())
                ? payload.getEmail()
                : generateSyntheticEmail("parent", parent.getId());

        User parentUser;
        if (userRepository.existsByEmail(parentEmail)) {
            parentUser = userRepository.findByEmail(parentEmail)
                    .orElseThrow(() -> new BadRequestException("User with email exists but could not be found"));
        } else {
            String actualPassword = (payload.getPassword() != null && !payload.getPassword().isBlank())
                    ? payload.getPassword()
                    : DEFAULT_PASSWORD;
            parentUser = User.builder()
                    .email(parentEmail)
                    .passwordHash(passwordEncoder.encode(actualPassword))
                    .fullName(payload.getFullName().trim())
                    .phone(payload.getPhone())
                    .emailVerified(true)
                    .isActive(true)
                    .build();
            parentUser = userRepository.save(parentUser);
        }

        parent.setUserId(parentUser.getId());
        parentRepository.save(parent);

        Role parentRole = roleRepository.findBySchoolIdAndName(schoolId, "PARENT")
                .orElseGet(() -> roleRepository.findBySchoolIdAndName(schoolId, "GUARDIAN")
                        .orElse(null));

        if (parentRole != null && !userSchoolRepository.existsByUserIdAndSchoolId(parentUser.getId(), schoolId)) {
            UserSchool userSchool = UserSchool.builder()
                    .userId(parentUser.getId())
                    .schoolId(schoolId)
                    .roleId(parentRole.getId())
                    .isActive(true)
                    .joinedAt(LocalDateTime.now())
                    .build();
            userSchoolRepository.save(userSchool);
        }

        log.info("User account linked for parent: {} in school {}", parentUser.getId(), schoolId);
        return parent;
    }

    private void linkExistingParent(UUID schoolId, UUID studentId, UUID parentId) {
        ParentStudent link = ParentStudent.builder()
                .parentId(parentId)
                .studentId(studentId)
                .isPrimary(true)
                .build();
        parentStudentRepository.save(link);
        log.info("Linked parent {} to student {} in school {}", parentId, studentId, schoolId);
    }

    @Transactional
    public StudentResponse updateStudent(UUID schoolId, UUID studentId, CreateStudentRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && !request.getEmail().equals(student.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("User with this email already exists");
            }
            if (studentRepository.existsBySchoolIdAndEmail(schoolId, request.getEmail())) {
                throw new BadRequestException("Student with this email already exists");
            }
            student.setEmail(request.getEmail());

            // Cascade email change to linked user account
            if (student.getUserId() != null) {
                User linkedUser = userRepository.findById(student.getUserId()).orElse(null);
                if (linkedUser != null) {
                    linkedUser.setEmail(request.getEmail());
                    userRepository.save(linkedUser);
                }
            }
        }

        if (request.getClassId() != null && !classRepository.existsById(request.getClassId())) {
            throw new BadRequestException("Class not found");
        }

        student.setFullName(request.getFullName());
        student.setPhone(request.getPhone());
        student.setDateOfBirth(request.getDateOfBirth());

        // Normalize and validate gender on update
        String gender = request.getGender();
        if (gender != null && !gender.isBlank()) {
            gender = gender.trim().toUpperCase();
            if (!gender.equals("MALE") && !gender.equals("FEMALE") && !gender.equals("OTHER")) {
                throw new BadRequestException("Invalid gender: " + gender + ". Must be MALE, FEMALE or OTHER.");
            }
        } else {
            gender = null;
        }
        student.setGender(gender);

        student.setAddress(request.getAddress());
        student.setClassId(request.getClassId());
        student.setParentName(request.getParentName());
        student.setParentEmail(request.getParentEmail());
        student.setParentPhone(request.getParentPhone());
        if (request.getPhotoUrl() != null) {
            student.setPhotoUrl(request.getPhotoUrl());
        }
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

        // Unlink payments to avoid FK constraint violations
        paymentRepository.unlinkByStudentId(studentId);

        // Remove user-school mapping if present
        if (student.getUserId() != null) {
            userSchoolRepository.findByUserIdAndSchoolId(student.getUserId(), schoolId)
                    .ifPresent(userSchoolRepository::delete);
        }

        // Hard delete student (DB cascades parent_students, attendance, grades, etc.)
        studentRepository.delete(student);
        log.info("Student hard deleted: {}", studentId);
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
