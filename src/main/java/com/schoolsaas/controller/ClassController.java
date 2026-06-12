package com.schoolsaas.controller;

import com.schoolsaas.dto.classroom.ClassRequest;
import com.schoolsaas.dto.classroom.ClassResponse;
import com.schoolsaas.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'class.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<ClassResponse>> getClasses(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(classService.getClasses(schoolId, search, pageable));
    }

    @GetMapping("/{classId}")
    @PreAuthorize("hasPermission(#schoolId, 'class.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ClassResponse> getClass(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId) {
        return ResponseEntity.ok(classService.getClass(schoolId, classId));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'class.create') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ClassResponse> createClass(
            @PathVariable UUID schoolId,
            @Valid @RequestBody ClassRequest request) {
        return ResponseEntity.ok(classService.createClass(schoolId, request));
    }

    @PutMapping("/{classId}")
    @PreAuthorize("hasPermission(#schoolId, 'class.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<ClassResponse> updateClass(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @Valid @RequestBody ClassRequest request) {
        return ResponseEntity.ok(classService.updateClass(schoolId, classId, request));
    }

    @DeleteMapping("/{classId}")
    @PreAuthorize("hasPermission(#schoolId, 'class.delete') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteClass(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId) {
        classService.deleteClass(schoolId, classId);
        return ResponseEntity.ok().build();
    }
}
