package com.schoolsaas.controller;

import com.schoolsaas.dto.school.SchoolSettingsRequest;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.School;
import com.schoolsaas.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/settings")
@RequiredArgsConstructor
public class SchoolSettingsController {

    private final SchoolRepository schoolRepository;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'school.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSettings(@PathVariable UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        Map<String, Object> settings = new HashMap<>();
        settings.put("schoolName", school.getName());
        settings.put("email", school.getEmail());
        settings.put("phone", school.getPhone());
        settings.put("address", school.getAddress());
        settings.put("logoUrl", school.getLogoUrl());

        Map<String, Object> config = school.getConfig() != null ? school.getConfig() : new HashMap<>();
        settings.put("primaryColor", config.getOrDefault("primaryColor", "#3b82f6"));
        settings.put("secondaryColor", config.getOrDefault("secondaryColor", "#8b5cf6"));
        settings.put("accentColor", config.getOrDefault("accentColor", "#10b981"));
        settings.put("currency", config.getOrDefault("currency", "NGN"));
        settings.put("timezone", config.getOrDefault("timezone", "Africa/Lagos"));
        settings.put("gradingScale", config.getOrDefault("gradingScale", null));
        settings.put("paymentAccounts", config.getOrDefault("paymentAccounts", new java.util.ArrayList<>()));
        settings.put("feeItems", config.getOrDefault("feeItems", new java.util.ArrayList<>()));

        return ResponseEntity.ok(settings);
    }

    @PutMapping
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @PathVariable UUID schoolId,
            @RequestBody SchoolSettingsRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        if (request.getSchoolName() != null && !request.getSchoolName().isBlank()) {
            school.setName(request.getSchoolName().trim());
        }
        if (request.getEmail() != null) {
            school.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            school.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            school.setAddress(request.getAddress());
        }
        if (request.getLogoUrl() != null) {
            school.setLogoUrl(request.getLogoUrl());
        }

        Map<String, Object> config = school.getConfig() != null
                ? new HashMap<>(school.getConfig())
                : new HashMap<>();

        if (request.getPrimaryColor() != null) {
            config.put("primaryColor", request.getPrimaryColor());
        }
        if (request.getSecondaryColor() != null) {
            config.put("secondaryColor", request.getSecondaryColor());
        }
        if (request.getAccentColor() != null) {
            config.put("accentColor", request.getAccentColor());
        }
        if (request.getCurrency() != null) {
            config.put("currency", request.getCurrency());
        }
        if (request.getTimezone() != null) {
            config.put("timezone", request.getTimezone());
        }
        if (request.getGradingScale() != null) {
            config.put("gradingScale", request.getGradingScale());
        }
        if (request.getPaymentAccounts() != null) {
            config.put("paymentAccounts", request.getPaymentAccounts());
        }
        if (request.getFeeItems() != null) {
            config.put("feeItems", request.getFeeItems());
        }

        school.setConfig(config);
        school = schoolRepository.save(school);

        return getSettings(schoolId);
    }
}
