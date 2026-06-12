package com.schoolsaas.controller;

import com.schoolsaas.model.BulkEnrollmentJob;
import com.schoolsaas.security.UserPrincipal;
import com.schoolsaas.service.BulkEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/bulk-enroll")
@RequiredArgsConstructor
public class BulkEnrollmentController {

    private final BulkEnrollmentService bulkEnrollmentService;

    @PostMapping("/preview")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'STUDENT_CREATE')")
    public ResponseEntity<BulkEnrollmentService.PreviewResult> previewFile(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(bulkEnrollmentService.previewFile(file));
    }

    @GetMapping("/fields")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'STUDENT_CREATE')")
    public ResponseEntity<List<Map<String, Object>>> getAvailableFields(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(bulkEnrollmentService.getAvailableFields());
    }

    @PostMapping("/process")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'STUDENT_CREATE')")
    public ResponseEntity<Map<String, Object>> processFile(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mapping") String mappingJson,
            @RequestParam(value = "entityType", defaultValue = "students") String entityType,
            @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {

        Map<String, String> columnMapping = parseMapping(mappingJson);
        columnMapping.put("__entityType", entityType);

        BulkEnrollmentJob job = bulkEnrollmentService.createJob(
                schoolId,
                userPrincipal.getId(),
                file.getOriginalFilename(),
                columnMapping
        );

        // Read file bytes BEFORE the async call — MultipartFile streams are
        // tied to the request thread and will be closed once this method returns.
        byte[] fileBytes = file.getBytes();
        bulkEnrollmentService.processJobAsync(job.getId(), fileBytes, file.getOriginalFilename());

        return ResponseEntity.ok(Map.of(
                "jobId", job.getId(),
                "status", "PROCESSING",
                "message", "Bulk enrollment started. Check job status for progress."
        ));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'STUDENT_VIEW')")
    public ResponseEntity<Page<BulkEnrollmentJob>> getJobs(
            @PathVariable UUID schoolId,
            Pageable pageable) {
        return ResponseEntity.ok(bulkEnrollmentService.getJobs(schoolId, pageable));
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'STUDENT_VIEW')")
    public ResponseEntity<BulkEnrollmentJob> getJob(
            @PathVariable UUID schoolId,
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(bulkEnrollmentService.getJob(jobId));
    }

    private Map<String, String> parseMapping(String mappingJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(mappingJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new com.schoolsaas.exception.BadRequestException("Invalid mapping format");
        }
    }
}
