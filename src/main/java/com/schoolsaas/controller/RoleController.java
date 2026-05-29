package com.schoolsaas.controller;

import com.schoolsaas.dto.role.CreateRoleRequest;
import com.schoolsaas.dto.role.RoleResponse;
import com.schoolsaas.model.Permission;
import com.schoolsaas.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/permissions")
    @PreAuthorize("hasPermission(#schoolId, 'permission.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<Permission>> getAllPermissions(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }

    @GetMapping("/permissions/grouped")
    @PreAuthorize("hasPermission(#schoolId, 'permission.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Map<String, List<Permission>>> getPermissionsByCategory(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(roleService.getPermissionsByCategory());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasPermission(#schoolId, 'role.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<RoleResponse>> getRoles(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(roleService.getRolesBySchool(schoolId));
    }

    @GetMapping("/roles/{roleId}")
    @PreAuthorize("hasPermission(#schoolId, 'role.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<RoleResponse> getRole(@PathVariable UUID schoolId, @PathVariable UUID roleId) {
        return ResponseEntity.ok(roleService.getRole(schoolId, roleId));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasPermission(#schoolId, 'role.create')")
    public ResponseEntity<RoleResponse> createRole(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(roleService.createRole(schoolId, request));
    }

    @PutMapping("/roles/{roleId}")
    @PreAuthorize("hasPermission(#schoolId, 'role.update')")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID schoolId,
            @PathVariable UUID roleId,
            @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(schoolId, roleId, request));
    }

    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("hasPermission(#schoolId, 'role.delete')")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID schoolId, @PathVariable UUID roleId) {
        roleService.deleteRole(schoolId, roleId);
        return ResponseEntity.ok().build();
    }
}
