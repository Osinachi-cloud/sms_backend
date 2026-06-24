package com.schoolsaas.controller;

import com.schoolsaas.dto.gradebook.GradebookEntryDto;
import com.schoolsaas.service.GradebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/gradebook")
@RequiredArgsConstructor
public class GradebookController {

    private final GradebookService gradebookService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<GradebookEntryDto>> getGradebook(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(gradebookService.getGradebook(
                schoolId, classId, subjectId, studentId, termId, sessionId, search, pageable));
    }
}
