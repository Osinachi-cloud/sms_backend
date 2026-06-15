package com.schoolsaas.controller;

import com.schoolsaas.dto.subject.SubjectRequest;
import com.schoolsaas.dto.subject.SubjectResponse;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'subject.read') or hasPermission(#schoolId, 'class.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<SubjectResponse>> getSubjects(@PathVariable UUID schoolId, Pageable pageable) {
        List<SubjectResponse> list = subjectService.getSubjects(schoolId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasPermission(#schoolId, 'subject.read') or hasPermission(#schoolId, 'student.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<SubjectResponse>> getSubjectsForStudent(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            Pageable pageable) {
        List<SubjectResponse> list = subjectService.getSubjectsForStudent(schoolId, studentId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/{subjectId}")
    @PreAuthorize("hasPermission(#schoolId, 'subject.read') or hasPermission(#schoolId, 'class.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<SubjectResponse> getSubject(@PathVariable UUID schoolId, @PathVariable UUID subjectId) {
        return ResponseEntity.ok(subjectService.getSubject(schoolId, subjectId));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'subject.create') or hasPermission(#schoolId, 'class.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<SubjectResponse> createSubject(@PathVariable UUID schoolId, @Valid @RequestBody SubjectRequest request) {
        UUID createdBy = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin() || SecurityUtils.hasPermission("subject.update");
        String createdByType = isAdmin ? "ADMIN" : "TEACHER";
        return ResponseEntity.ok(subjectService.createSubject(schoolId, request, createdBy, createdByType));
    }

    @PutMapping("/{subjectId}")
    @PreAuthorize("hasPermission(#schoolId, 'subject.update') or hasPermission(#schoolId, 'cms.content.edit.any') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable UUID schoolId, @PathVariable UUID subjectId, @Valid @RequestBody SubjectRequest request) {
        boolean isAdmin = SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin() || SecurityUtils.hasPermission("subject.update");
        return ResponseEntity.ok(subjectService.updateSubject(schoolId, subjectId, request, isAdmin));
    }

    @DeleteMapping("/{subjectId}")
    @PreAuthorize("hasPermission(#schoolId, 'subject.delete') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteSubject(@PathVariable UUID schoolId, @PathVariable UUID subjectId) {
        subjectService.deleteSubject(schoolId, subjectId);
        return ResponseEntity.ok().build();
    }
}
