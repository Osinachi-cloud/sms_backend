package com.schoolsaas.controller;

import com.schoolsaas.dto.school.CreateSchoolRequest;
import com.schoolsaas.dto.school.SchoolResponse;
import com.schoolsaas.service.SchoolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/schools")
@RequiredArgsConstructor
public class AdminSchoolController {

    private final SchoolService schoolService;

    @PostMapping
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<SchoolResponse> createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        return ResponseEntity.ok(schoolService.createSchool(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<SchoolResponse>> getAllSchools(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(schoolService.searchSchools(search, pageable));
        }
        return ResponseEntity.ok(schoolService.getAllSchools(pageable));
    }

    @GetMapping("/{schoolId}")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<SchoolResponse> getSchool(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(schoolService.getSchool(schoolId));
    }

    @PutMapping("/{schoolId}")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<SchoolResponse> updateSchool(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateSchoolRequest request) {
        return ResponseEntity.ok(schoolService.updateSchool(schoolId, request));
    }

    @PutMapping("/{schoolId}/deactivate")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deactivateSchool(@PathVariable UUID schoolId) {
        schoolService.deactivateSchool(schoolId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{schoolId}/reactivate")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> reactivateSchool(@PathVariable UUID schoolId) {
        schoolService.reactivateSchool(schoolId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{schoolId}/assign-super-admin")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> assignSuperAdmin(
            @PathVariable UUID schoolId,
            @RequestBody Map<String, UUID> body) {
        schoolService.assignSuperAdmin(schoolId, body.get("userId"));
        return ResponseEntity.ok().build();
    }
}
