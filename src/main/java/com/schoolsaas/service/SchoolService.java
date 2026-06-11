package com.schoolsaas.service;

import com.schoolsaas.dto.school.CreateSchoolRequest;
import com.schoolsaas.dto.school.SchoolResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Role;
import com.schoolsaas.model.School;
import com.schoolsaas.model.User;
import com.schoolsaas.model.UserSchool;
import com.schoolsaas.repository.RoleRepository;
import com.schoolsaas.repository.SchoolRepository;
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
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserSchoolRepository userSchoolRepository;
    private final RoleRepository roleRepository;
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
        return SchoolResponse.fromEntity(school);
    }

    @Transactional(readOnly = true)
    public Page<SchoolResponse> getAllSchools(Pageable pageable) {
        return schoolRepository.findAllActive(pageable)
                .map(SchoolResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<SchoolResponse> searchSchools(String search, Pageable pageable) {
        return schoolRepository.searchByNameOrCode(search, pageable)
                .map(SchoolResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public SchoolResponse getSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));
        return SchoolResponse.fromEntity(school);
    }

    @Transactional
    public SchoolResponse updateSchool(UUID schoolId, CreateSchoolRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        if (request.getSubdomain() != null && !request.getSubdomain().equals(school.getSubdomain())) {
            if (schoolRepository.existsBySubdomain(request.getSubdomain())) {
                throw new BadRequestException("Subdomain already taken");
            }
            school.setSubdomain(request.getSubdomain());
        }

        school.setName(request.getName());
        school.setEmail(request.getEmail());
        school.setPhone(request.getPhone());
        school.setAddress(request.getAddress());
        if (request.getLogoUrl() != null) {
            school.setLogoUrl(request.getLogoUrl());
        }
        if (request.getConfig() != null) {
            school.setConfig(request.getConfig());
        }

        school = schoolRepository.save(school);
        log.info("School updated: {}", school.getId());
        return SchoolResponse.fromEntity(school);
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
}
