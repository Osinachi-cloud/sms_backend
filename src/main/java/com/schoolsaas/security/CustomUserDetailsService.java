package com.schoolsaas.security;

import com.schoolsaas.repository.TemporaryUserPermissionRepository;
import com.schoolsaas.repository.UserRepository;
import com.schoolsaas.repository.UserSchoolRepository;
import com.schoolsaas.repository.RolePermissionRepository;
import com.schoolsaas.model.User;
import com.schoolsaas.model.UserSchool;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserSchoolRepository userSchoolRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final TemporaryUserPermissionRepository temporaryUserPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return buildUserPrincipal(user, null);
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadUserById(UUID userId, UUID schoolId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        return buildUserPrincipal(user, schoolId);
    }

    private UserPrincipal buildUserPrincipal(User user, UUID schoolId) {
        Set<String> permissions = new HashSet<>();
        UUID roleId = null;
        String schoolRoleName = null;

        if (schoolId != null && !user.isPlatformAdmin()) {
            Optional<UserSchool> userSchool = userSchoolRepository.findByUserIdAndSchoolId(user.getId(), schoolId);
            if (userSchool.isPresent() && userSchool.get().getRoleId() != null) {
                UserSchool us = userSchool.get();
                roleId = us.getRoleId();
                permissions = rolePermissionRepository.findPermissionKeysByRoleId(roleId);
                if (us.getRole() != null) {
                    schoolRoleName = us.getRole().getName();
                }
            }
            // Load temporary permissions granted by super admin
            List<String> tempPermissions = temporaryUserPermissionRepository
                    .findByUserIdAndSchoolIdAndExpiresAtAfter(user.getId(), schoolId, LocalDateTime.now())
                    .stream()
                    .map(tp -> tp.getPermissionKey())
                    .toList();
            permissions.addAll(tempPermissions);
        }

        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .fullName(user.getFullName())
                .platformRole(user.getPlatformRole())
                .currentSchoolId(schoolId)
                .currentRoleId(roleId)
                .schoolRole(schoolRoleName)
                .permissions(permissions)
                .active(user.getIsActive())
                .build();
    }
}
