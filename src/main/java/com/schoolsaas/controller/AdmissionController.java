package com.schoolsaas.controller;

import com.schoolsaas.dto.admission.AdmissionApplicationDto;
import com.schoolsaas.service.AdmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/admissions")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionService admissionService;

    @PostMapping
    public ResponseEntity<AdmissionApplicationDto> submitApplication(@PathVariable UUID schoolId, @RequestBody AdmissionApplicationDto dto) {
        return ResponseEntity.ok(admissionService.submitApplication(schoolId, dto));
    }

    @GetMapping
    public ResponseEntity<Page<AdmissionApplicationDto>> listApplications(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(admissionService.listApplications(schoolId, status, pageable));
    }

    @PostMapping("/{applicationId}/review")
    public ResponseEntity<AdmissionApplicationDto> reviewApplication(
            @PathVariable UUID applicationId,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        String notes = (String) body.get("reviewNotes");
        BigDecimal examScore = body.get("examScore") != null ? new BigDecimal(body.get("examScore").toString()) : null;
        BigDecimal interviewScore = body.get("interviewScore") != null ? new BigDecimal(body.get("interviewScore").toString()) : null;
        return ResponseEntity.ok(admissionService.reviewApplication(applicationId, status, notes, examScore, interviewScore));
    }
}
