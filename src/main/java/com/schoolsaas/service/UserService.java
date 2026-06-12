package com.schoolsaas.service;

import com.schoolsaas.dto.user.CreateSchoolUserRequest;
import com.schoolsaas.dto.user.SchoolUserResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Role;
import com.schoolsaas.model.User;
import com.schoolsaas.model.UserSchool;
import com.schoolsaas.repository.RoleRepository;
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
public class UserService {

    private final UserRepository userRepository;
    private final UserSchoolRepository userSchoolRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<SchoolUserResponse> getSchoolUsers(UUID schoolId, Pageable pageable) {
        return userSchoolRepository.findBySchoolIdAndIsActiveTrue(schoolId, pageable)
                .map(us -> {
                    User user = userRepository.findById(us.getUserId()).orElse(null);
                    Role role = us.getRoleId() != null ? roleRepository.findById(us.getRoleId()).orElse(null) : null;
                    if (user == null) return null;
                    return SchoolUserResponse.builder()
                            .id(user.getId())
                            .fullName(user.getFullName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .roleId(role != null ? role.getId() : null)
                            .roleName(role != null ? role.getName() : null)
                            .isActive(us.getIsActive())
                            .joinedAt(us.getJoinedAt())
                            .createdAt(us.getCreatedAt())
                            .build();
                });
    }

    @Transactional
    public SchoolUserResponse createSchoolUser(UUID schoolId, CreateSchoolUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("User with this email already exists");
        }

        Role role = roleRepository.findBySchoolIdAndName(schoolId, request.getRoleName())
                .orElseThrow(() -> new BadRequestException("Role not found: " + request.getRoleName()));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .emailVerified(true)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        UserSchool userSchool = UserSchool.builder()
                .userId(user.getId())
                .schoolId(schoolId)
                .roleId(role.getId())
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();
        userSchool = userSchoolRepository.save(userSchool);

        log.info("School user created: {} with role {} in school {}", user.getId(), role.getName(), schoolId);

        return SchoolUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleId(role.getId())
                .roleName(role.getName())
                .isActive(userSchool.getIsActive())
                .joinedAt(userSchool.getJoinedAt())
                .createdAt(userSchool.getCreatedAt())
                .build();
    }
}
