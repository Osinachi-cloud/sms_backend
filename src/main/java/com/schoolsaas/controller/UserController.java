package com.schoolsaas.controller;

import com.schoolsaas.dto.user.CreateSchoolUserRequest;
import com.schoolsaas.dto.user.SchoolUserResponse;
import com.schoolsaas.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'user.read') or hasPermission(#schoolId, 'role.read')")
    public ResponseEntity<Page<SchoolUserResponse>> getSchoolUsers(
            @PathVariable UUID schoolId,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getSchoolUsers(schoolId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'user.create') or hasPermission(#schoolId, 'role.create')")
    public ResponseEntity<SchoolUserResponse> createSchoolUser(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateSchoolUserRequest request) {
        return ResponseEntity.ok(userService.createSchoolUser(schoolId, request));
    }
}
