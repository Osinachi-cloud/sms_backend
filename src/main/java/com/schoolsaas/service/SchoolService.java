package com.schoolsaas.service;

import com.schoolsaas.dto.school.CreateSchoolRequest;
import com.schoolsaas.dto.school.SchoolResponse;
import com.schoolsaas.dto.school.UpdateSchoolRequest;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserSchoolRepository userSchoolRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SchoolResponse createSchool(CreateSchoolRequest request) {
        if (request.getSubdomain() != null && schoolRepository.existsBySubdomain(request.getSubdomain())) {
            throw new BadRequestException("Subdomain already taken");
        }

        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new BadRequestException("Admin email already registered");
        }

        String code = generateSchoolCode(request.getName());

        School school = School.builder()
                .name(request.getName())
                .subdomain(request.getSubdomain())
                .code(code)
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .logoUrl(request.getLogoUrl())
                .config(request.getConfig() != null ? request.getConfig() : new java.util.HashMap<>())
                .status("ACTIVE")
                .build();

        school = schoolRepository.save(school);
        createDefaultRoles(school.getId());
        assignDefaultPermissions(school.getId());

        // Create Super Admin User
        User adminUser = User.builder()
                .email(request.getAdminEmail())
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .fullName(request.getAdminFullName())
                .emailVerified(true)
                .isActive(true)
                .build();
        adminUser = userRepository.save(adminUser);

        // Assign Super Admin Role
        assignSuperAdmin(school.getId(), adminUser.getId());

        log.info("School created: {} ({}) with admin: {}", school.getName(), school.getCode(), adminUser.getEmail());
        return buildSchoolResponse(school);
    }

    @Transactional(readOnly = true)
    public Page<SchoolResponse> getAllSchools(Pageable pageable) {
        return schoolRepository.findAllActive(pageable)
                .map(this::buildSchoolResponse);
    }

    @Transactional(readOnly = true)
    public Page<SchoolResponse> searchSchools(String search, Pageable pageable) {
        return schoolRepository.searchByNameOrCode(search, pageable)
                .map(this::buildSchoolResponse);
    }

    @Transactional(readOnly = true)
    public SchoolResponse getSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));
        return buildSchoolResponse(school);
    }

    @Transactional
    public SchoolResponse updateSchool(UUID schoolId, UpdateSchoolRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        if (request.getSubdomain() != null && !request.getSubdomain().equals(school.getSubdomain())) {
            if (schoolRepository.existsBySubdomain(request.getSubdomain())) {
                throw new BadRequestException("Subdomain already taken");
            }
            school.setSubdomain(request.getSubdomain());
        }

        if (request.getName() != null) {
            school.setName(request.getName());
        }
        if (request.getEmail() != null) {
            school.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            school.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            school.setAddress(request.getAddress());
        }
        if (request.getLogoUrl() != null) {
            school.setLogoUrl(request.getLogoUrl());
        }
        if (request.getConfig() != null) {
            school.setConfig(request.getConfig());
        }

        school = schoolRepository.save(school);

        // Handle admin updates
        if (request.getAdminUserId() != null || request.getAdminEmail() != null || request.getAdminFullName() != null || request.getAdminPassword() != null) {
            updateSchoolAdmin(schoolId, request);
        }

        log.info("School updated: {}", school.getId());
        return buildSchoolResponse(school);
    }

    @Transactional
    public void updateSchoolAdmin(UUID schoolId, UpdateSchoolRequest request) {
        Role superAdminRole = roleRepository.findBySchoolIdAndName(schoolId, "SUPER_ADMIN")
                .orElseThrow(() -> new BadRequestException("SUPER_ADMIN role not found for school"));

        UserSchool userSchool = userSchoolRepository.findBySchoolIdAndRoleId(schoolId, superAdminRole.getId())
                .stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No SUPER_ADMIN found for this school"));

        User admin = userRepository.findById(userSchool.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userSchool.getUserId()));

        String oldEmail = admin.getEmail();

        if (request.getAdminFullName() != null) {
            admin.setFullName(request.getAdminFullName());
        }

        if (request.getAdminEmail() != null && !request.getAdminEmail().equalsIgnoreCase(oldEmail)) {
            if (userRepository.existsByEmail(request.getAdminEmail())) {
                throw new BadRequestException("Admin email already registered by another user");
            }
            admin.setEmail(request.getAdminEmail());
            // Cascade email change across all tables
            cascadeEmailChange(oldEmail, request.getAdminEmail());
        }

        if (request.getAdminPassword() != null && !request.getAdminPassword().isBlank()) {
            admin.setPasswordHash(passwordEncoder.encode(request.getAdminPassword()));
        }

        userRepository.save(admin);
        log.info("School admin updated for school {}. Email changed from {} to {}", schoolId, oldEmail, admin.getEmail());
    }

    private void cascadeEmailChange(String oldEmail, String newEmail) {
        if (oldEmail == null || oldEmail.isBlank() || newEmail == null || newEmail.isBlank()) {
            return;
        }
        int teachersUpdated = teacherRepository.updateEmailByEmail(oldEmail, newEmail);
        int studentsUpdated = studentRepository.updateEmailByEmail(oldEmail, newEmail);
        int studentsParentUpdated = studentRepository.updateParentEmailByParentEmail(oldEmail, newEmail);
        int parentsUpdated = parentRepository.updateEmailByEmail(oldEmail, newEmail);
        int admissionsUpdated = admissionApplicationRepository.updateEmailByEmail(oldEmail, newEmail);
        int admissionsGuardianUpdated = admissionApplicationRepository.updateGuardianEmailByEmail(oldEmail, newEmail);
        log.info("Cascaded email change from {} to {}. Teachers: {}, Students: {}, StudentParents: {}, Parents: {}, Admissions: {}, AdmissionsGuardian: {}",
                oldEmail, newEmail, teachersUpdated, studentsUpdated, studentsParentUpdated, parentsUpdated, admissionsUpdated, admissionsGuardianUpdated);
    }

    @Transactional
    public void deactivateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        school.setStatus("INACTIVE");
        schoolRepository.save(school);
        log.info("School deactivated: {}", schoolId);
    }

    @Transactional
    public void reactivateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        school.setStatus("ACTIVE");
        schoolRepository.save(school);
        log.info("School reactivated: {}", schoolId);
    }

    @Transactional
    public void assignSuperAdmin(UUID schoolId, UUID userId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role superAdminRole = roleRepository.findBySchoolIdAndName(schoolId, "SUPER_ADMIN")
                .orElseThrow(() -> new BadRequestException("SUPER_ADMIN role not found for school"));

        var existingMapping = userSchoolRepository.findByUserIdAndSchoolId(userId, schoolId);
        if (existingMapping.isPresent()) {
            var mapping = existingMapping.get();
            mapping.setRoleId(superAdminRole.getId());
            mapping.setIsActive(true);
            userSchoolRepository.save(mapping);
        } else {
            UserSchool userSchool = UserSchool.builder()
                    .userId(userId)
                    .schoolId(schoolId)
                    .roleId(superAdminRole.getId())
                    .isActive(true)
                    .joinedAt(LocalDateTime.now())
                    .build();
            userSchoolRepository.save(userSchool);
        }

        log.info("SUPER_ADMIN assigned: user {} to school {}", userId, schoolId);
    }

    private SchoolResponse buildSchoolResponse(School school) {
        SchoolResponse response = SchoolResponse.fromEntity(school);

        // Find SUPER_ADMIN for this school
        Role superAdminRole = roleRepository.findBySchoolIdAndName(school.getId(), "SUPER_ADMIN").orElse(null);
        if (superAdminRole != null) {
            userSchoolRepository.findBySchoolIdAndRoleId(school.getId(), superAdminRole.getId())
                    .stream().findFirst()
                    .flatMap(us -> userRepository.findById(us.getUserId()))
                    .ifPresent(admin -> response.setAdmin(SchoolResponse.SchoolAdminInfo.builder()
                            .id(admin.getId())
                            .fullName(admin.getFullName())
                            .email(admin.getEmail())
                            .build()));
        }

        return response;
    }

    private String generateSchoolCode(String name) {
        String prefix = name.replaceAll("[^A-Za-z]", "")
                .toUpperCase()
                .substring(0, Math.min(name.length(), 3));

        String code;
        int attempts = 0;
        do {
            code = prefix + String.format("%04d", (int) (Math.random() * 10000));
            attempts++;
        } while (schoolRepository.existsByCode(code) && attempts < 100);

        return code;
    }

    private void createDefaultRoles(UUID schoolId) {
        String[] roles = {"SUPER_ADMIN", "ADMIN", "TEACHER", "STUDENT", "PARENT"};
        for (String roleName : roles) {
            Role role = Role.builder()
                    .schoolId(schoolId)
                    .name(roleName)
                    .description("Default " + roleName + " role")
                    .isSystemRole(true)
                    .isActive(true)
                    .build();
            roleRepository.save(role);
        }
    }

    private void assignDefaultPermissions(UUID schoolId) {
        Role superAdminRole = roleRepository.findBySchoolIdAndName(schoolId, "SUPER_ADMIN")
                .orElse(null);
        Role adminRole = roleRepository.findBySchoolIdAndName(schoolId, "ADMIN")
                .orElse(null);

        if (superAdminRole != null) {
            List<Permission> allPermissions = permissionRepository.findAll();
            for (Permission permission : allPermissions) {
                RolePermission rp = RolePermission.builder()
                        .roleId(superAdminRole.getId())
                        .permissionKey(permission.getKey())
                        .build();
                rolePermissionRepository.save(rp);
            }
        }

        if (adminRole != null) {
            List<String> adminKeys = List.of(
                    "student.read", "student.create", "student.update", "student.delete", "student.bulk.enroll",
                    "student.grades.read", "student.grades.manage", "student.attendance.read", "student.attendance.manage",
                    "teacher.read", "teacher.create", "teacher.update", "teacher.delete", "teacher.assign.class",
                    "class.read", "class.create", "class.update", "class.delete",
                    "subject.read", "subject.create", "subject.update", "subject.delete",
                    "cms.folder.read", "cms.folder.create", "cms.content.read", "cms.content.create",
                    "cms.content.edit", "cms.content.edit.any", "cms.content.approve", "cms.content.publish",
                    "fee.read", "fee.create", "fee.update", "payment.read", "payment.create",
                    "payment.gateway.manage", "payment.gateway.switch",
                    "analytics.academic.view", "analytics.finance.view", "school.read", "school.update",
                    "role.read", "role.create", "role.delete",
                    "user.read", "user.create"
            );
            for (String key : adminKeys) {
                if (permissionRepository.existsByKey(key)) {
                    RolePermission rp = RolePermission.builder()
                            .roleId(adminRole.getId())
                            .permissionKey(key)
                            .build();
                    rolePermissionRepository.save(rp);
                }
            }
        }

        // Teacher permissions
        Role teacherRole = roleRepository.findBySchoolIdAndName(schoolId, "TEACHER")
                .orElse(null);
        if (teacherRole != null) {
            List<String> teacherKeys = List.of(
                    "student.read", "student.grades.read", "student.grades.manage",
                    "student.attendance.read", "student.attendance.manage",
                    "class.read", "subject.read",
                    "cms.folder.read", "cms.folder.create", "cms.content.read", "cms.content.create",
                    "cms.content.edit", "cms.content.delete", "cms.content.submit"
            );
            for (String key : teacherKeys) {
                if (permissionRepository.existsByKey(key)) {
                    RolePermission rp = RolePermission.builder()
                            .roleId(teacherRole.getId())
                            .permissionKey(key)
                            .build();
                    rolePermissionRepository.save(rp);
                }
            }
        }

        // Student permissions
        Role studentRole = roleRepository.findBySchoolIdAndName(schoolId, "STUDENT")
                .orElse(null);
        if (studentRole != null) {
            List<String> studentKeys = List.of(
                    "student.grades.read", "student.attendance.read",
                    "cms.content.read", "fee.read", "payment.read"
            );
            for (String key : studentKeys) {
                if (permissionRepository.existsByKey(key)) {
                    RolePermission rp = RolePermission.builder()
                            .roleId(studentRole.getId())
                            .permissionKey(key)
                            .build();
                    rolePermissionRepository.save(rp);
                }
            }
        }
    }
}
