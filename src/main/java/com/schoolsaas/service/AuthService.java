package com.schoolsaas.service;

import com.schoolsaas.dto.auth.AuthResponse;
import com.schoolsaas.dto.auth.LoginRequest;
import com.schoolsaas.dto.auth.RegisterRequest;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.UnauthorizedException;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSchoolRepository userSchoolRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .emailVerified(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return generateAuthResponse(user, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String login = request.getEmail() != null ? request.getEmail().trim() : "";
        User user = userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login))
                .orElseThrow(() -> new UnauthorizedException("Invalid email/username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email/username or password");
        }

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        UUID resolvedSchoolId = resolveSchoolContext(user, request.getSchoolId());

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return generateAuthResponse(user, resolvedSchoolId);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        if (!"REFRESH".equals(tokenProvider.getTokenType(refreshToken))) {
            throw new UnauthorizedException("Invalid token type");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (!storedToken.isValid()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        User user = storedToken.getUser();
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return generateAuthResponse(user, null);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("User logged out: {}", userId);
    }

    @Transactional
    public AuthResponse switchSchool(UUID userId, String schoolIdStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        UUID schoolId = schoolIdStr != null ? UUID.fromString(schoolIdStr) : null;

        if (schoolId != null) {
            boolean hasAccess = userSchoolRepository.findByUserIdAndSchoolId(userId, schoolId)
                    .map(UserSchool::getIsActive)
                    .orElse(false);
            if (!hasAccess && !user.isPlatformAdmin()) {
                throw new UnauthorizedException("No access to this school");
            }
        }

        return generateAuthResponse(user, schoolId);
    }

    private AuthResponse generateAuthResponse(User user, UUID schoolId) {
        Set<String> permissions = resolvePermissions(user.getId(), schoolId);

        String accessToken = tokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail() != null ? user.getEmail() : user.getUsername(),
                user.getPlatformRole(),
                schoolId,
                permissions
        );

        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        RefreshToken tokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpiration() / 1000))
                .build();
        refreshTokenRepository.save(tokenEntity);

        List<AuthResponse.SchoolInfo> schools = getSchoolsForUser(user.getId());

        UUID studentId = schoolId != null
                ? studentRepository.findByUserId(user.getId()).map(s -> s.getId()).orElse(null)
                : null;

        List<AuthResponse.ChildInfo> children = new java.util.ArrayList<>();
        if (schoolId != null) {
            var parentOpt = parentRepository.findByUserIdAndSchoolId(user.getId(), schoolId);
            if (parentOpt.isPresent()) {
                var parent = parentOpt.get();
                List<UUID> childIds = parentStudentRepository.findByParentId(parent.getId()).stream()
                        .map(ParentStudent::getStudentId)
                        .collect(java.util.stream.Collectors.toList());
                if (!childIds.isEmpty()) {
                    List<Student> childStudents = studentRepository.findAllById(childIds);
                    for (Student s : childStudents) {
                        children.add(AuthResponse.ChildInfo.builder()
                                .id(s.getId())
                                .fullName(s.getFullName())
                                .className(s.getSchoolClass() != null ? s.getSchoolClass().getName() : null)
                                .classId(s.getClassId())
                                .build());
                    }
                }
            }
        }

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .platformRole(user.getPlatformRole())
                        .studentId(studentId)
                        .children(children.isEmpty() ? null : children)
                        .schools(schools)
                        .build())
                .build();
    }

    private Set<String> resolvePermissions(UUID userId, UUID schoolId) {
        if (schoolId == null) {
            return Set.of();
        }
        return userSchoolRepository.findByUserIdAndSchoolId(userId, schoolId)
                .map(UserSchool::getRoleId)
                .map(rolePermissionRepository::findPermissionKeysByRoleId)
                .orElse(Set.of());
    }

    private UUID resolveSchoolContext(User user, UUID requestedSchoolId) {
        if (user.isPlatformAdmin()) {
            return null;
        }

        List<UserSchool> memberships = userSchoolRepository.findByUserId(user.getId()).stream()
                .filter(UserSchool::getIsActive)
                .collect(Collectors.toList());

        if (requestedSchoolId != null) {
            boolean hasAccess = memberships.stream().anyMatch(us -> us.getSchoolId().equals(requestedSchoolId));
            if (!hasAccess) {
                throw new UnauthorizedException("No access to this school");
            }
            return requestedSchoolId;
        }

        if (memberships.size() == 1) {
            return memberships.get(0).getSchoolId();
        }

        return null;
    }

    private List<AuthResponse.SchoolInfo> getSchoolsForUser(UUID userId) {
        List<UserSchool> userSchools = userSchoolRepository.findByUserId(userId);

        return userSchools.stream()
                .filter(us -> us.getIsActive())
                .map(us -> {
                    var school = schoolRepository.findById(us.getSchoolId()).orElse(null);
                    if (school == null || !school.isActive()) return null;

                    Set<String> permissions = us.getRoleId() != null
                            ? rolePermissionRepository.findPermissionKeysByRoleId(us.getRoleId())
                            : Set.of();

                    String roleName = us.getRoleId() != null
                            ? roleRepository.findById(us.getRoleId()).map(r -> r.getName()).orElse(null)
                            : null;

                    return AuthResponse.SchoolInfo.builder()
                            .id(school.getId())
                            .name(school.getName())
                            .code(school.getCode())
                            .logoUrl(school.getLogoUrl())
                            .roleName(roleName)
                            .permissions(permissions)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
