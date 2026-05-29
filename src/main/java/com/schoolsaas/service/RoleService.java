package com.schoolsaas.service;

import com.schoolsaas.dto.role.CreateRoleRequest;
import com.schoolsaas.dto.role.RoleResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Permission;
import com.schoolsaas.model.Role;
import com.schoolsaas.model.RolePermission;
import com.schoolsaas.repository.PermissionRepository;
import com.schoolsaas.repository.RolePermissionRepository;
import com.schoolsaas.repository.RoleRepository;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllOrderedByCategory();
    }

    @Transactional(readOnly = true)
    public Map<String, List<Permission>> getPermissionsByCategory() {
        return permissionRepository.findAllOrderedByCategory().stream()
                .collect(Collectors.groupingBy(Permission::getCategory));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRolesBySchool(UUID schoolId) {
        return roleRepository.findBySchoolIdAndIsActiveTrue(schoolId).stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(UUID schoolId, UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (!role.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Role", "id", roleId);
        }

        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse createRole(UUID schoolId, CreateRoleRequest request) {
        if (roleRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new BadRequestException("Role with this name already exists");
        }

        validatePermissions(request.getPermissions());

        Role role = Role.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .description(request.getDescription())
                .isSystemRole(false)
                .isActive(true)
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();

        role = roleRepository.save(role);
        assignPermissionsToRole(role.getId(), request.getPermissions());

        log.info("Role created: {} for school {}", request.getName(), schoolId);
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(UUID schoolId, UUID roleId, CreateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (!role.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Role", "id", roleId);
        }

        if (role.getIsSystemRole()) {
            throw new BadRequestException("Cannot modify system role");
        }

        if (!role.getName().equals(request.getName()) &&
            roleRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new BadRequestException("Role with this name already exists");
        }

        validatePermissions(request.getPermissions());

        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role = roleRepository.save(role);

        rolePermissionRepository.deleteByRoleId(roleId);
        assignPermissionsToRole(roleId, request.getPermissions());

        log.info("Role updated: {} for school {}", roleId, schoolId);
        return toRoleResponse(role);
    }

    @Transactional
    public void deleteRole(UUID schoolId, UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (!role.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Role", "id", roleId);
        }

        if (role.getIsSystemRole()) {
            throw new BadRequestException("Cannot delete system role");
        }

        role.setIsActive(false);
        roleRepository.save(role);
        log.info("Role deleted: {} for school {}", roleId, schoolId);
    }

    private void validatePermissions(Set<String> permissionKeys) {
        Set<String> existingKeys = permissionRepository.findExistingKeys(permissionKeys);
        Set<String> invalidKeys = new HashSet<>(permissionKeys);
        invalidKeys.removeAll(existingKeys);

        if (!invalidKeys.isEmpty()) {
            throw new BadRequestException("Invalid permission keys: " + invalidKeys);
        }
    }

    private void assignPermissionsToRole(UUID roleId, Set<String> permissionKeys) {
        for (String key : permissionKeys) {
            RolePermission rp = RolePermission.builder()
                    .roleId(roleId)
                    .permissionKey(key)
                    .build();
            rolePermissionRepository.save(rp);
        }
    }

    private RoleResponse toRoleResponse(Role role) {
        Set<String> permissions = rolePermissionRepository.findPermissionKeysByRoleId(role.getId());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.getIsSystemRole())
                .permissions(permissions)
                .createdAt(role.getCreatedAt())
                .build();
    }
}
