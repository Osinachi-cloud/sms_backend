package com.schoolsaas.controller;

import com.schoolsaas.model.GradingScheme;
import com.schoolsaas.service.GradingSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/grading-schemes")
@RequiredArgsConstructor
public class GradingSchemeAdminController {

    private final GradingSchemeService gradingSchemeService;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<GradingScheme>> getAllSchemes(@RequestHeader("X-School-Id") UUID schoolId) {
        return ResponseEntity.ok(gradingSchemeService.getAllSchemes(schoolId));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<GradingScheme> createScheme(@RequestHeader("X-School-Id") UUID schoolId, @RequestBody GradingScheme scheme) {
        return ResponseEntity.ok(gradingSchemeService.createScheme(schoolId, scheme));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<GradingScheme> updateScheme(@RequestHeader("X-School-Id") UUID schoolId, @PathVariable UUID id, @RequestBody GradingScheme scheme) {
        return ResponseEntity.ok(gradingSchemeService.updateScheme(schoolId, id, scheme));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteScheme(@RequestHeader("X-School-Id") UUID schoolId, @PathVariable UUID id) {
        gradingSchemeService.deleteScheme(schoolId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/set-default")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> setDefault(@RequestHeader("X-School-Id") UUID schoolId, @PathVariable UUID id) {
        gradingSchemeService.setDefault(schoolId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/subjects/{subjectId}/assign-scheme")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> assignToSubject(
            @RequestHeader("X-School-Id") UUID schoolId,
            @PathVariable UUID subjectId,
            @RequestBody(required = false) UUID gradingSchemeId) {
        gradingSchemeService.assignToSubject(schoolId, subjectId, gradingSchemeId);
        return ResponseEntity.ok().build();
    }
}
