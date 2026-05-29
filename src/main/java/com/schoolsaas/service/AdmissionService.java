package com.schoolsaas.service;

import com.schoolsaas.dto.admission.AdmissionApplicationDto;
import com.schoolsaas.model.AdmissionApplication;
import com.schoolsaas.repository.AdmissionApplicationRepository;
import com.schoolsaas.repository.ClassRepository;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdmissionService {

    private final AdmissionApplicationRepository applicationRepository;
    private final ClassRepository classRepository;
    private final NotificationService notificationService;

    @Transactional
    public AdmissionApplicationDto submitApplication(UUID schoolId, AdmissionApplicationDto dto) {
        String appNumber = "APP-" + System.currentTimeMillis();
        AdmissionApplication app = AdmissionApplication.builder()
                .schoolId(schoolId)
                .applicationNumber(appNumber)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .previousSchool(dto.getPreviousSchool())
                .lastClassCompleted(dto.getLastClassCompleted())
                .guardianName(dto.getGuardianName())
                .guardianEmail(dto.getGuardianEmail())
                .guardianPhone(dto.getGuardianPhone())
                .guardianRelationship(dto.getGuardianRelationship())
                .intendedClassId(dto.getIntendedClassId())
                .metadata(dto.getMetadata())
                .build();
        app = applicationRepository.save(app);
        return mapToDto(app);
    }

    public Page<AdmissionApplicationDto> listApplications(UUID schoolId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return applicationRepository.findBySchoolIdAndStatus(schoolId, status, pageable).map(this::mapToDto);
        }
        return applicationRepository.findBySchoolId(schoolId, pageable).map(this::mapToDto);
    }

    @Transactional
    public AdmissionApplicationDto reviewApplication(UUID applicationId, String status, String reviewNotes, Double examScore, Double interviewScore) {
        AdmissionApplication app = applicationRepository.findById(applicationId).orElseThrow();
        app.setStatus(status);
        app.setReviewNotes(reviewNotes);
        app.setExamScore(examScore);
        app.setInterviewScore(interviewScore);
        app.setReviewedBy(SecurityUtils.getCurrentUserId());
        app.setReviewedAt(LocalDateTime.now());
        app = applicationRepository.save(app);

        if (app.getEmail() != null) {
            // Could send email notification here
        }

        return mapToDto(app);
    }

    private AdmissionApplicationDto mapToDto(AdmissionApplication app) {
        AdmissionApplicationDto dto = new AdmissionApplicationDto();
        dto.setId(app.getId());
        dto.setApplicationNumber(app.getApplicationNumber());
        dto.setFirstName(app.getFirstName());
        dto.setLastName(app.getLastName());
        dto.setDateOfBirth(app.getDateOfBirth());
        dto.setGender(app.getGender());
        dto.setEmail(app.getEmail());
        dto.setPhone(app.getPhone());
        dto.setAddress(app.getAddress());
        dto.setPreviousSchool(app.getPreviousSchool());
        dto.setLastClassCompleted(app.getLastClassCompleted());
        dto.setGuardianName(app.getGuardianName());
        dto.setGuardianEmail(app.getGuardianEmail());
        dto.setGuardianPhone(app.getGuardianPhone());
        dto.setGuardianRelationship(app.getGuardianRelationship());
        dto.setIntendedClassId(app.getIntendedClassId());
        if (app.getIntendedClassId() != null) {
            classRepository.findById(app.getIntendedClassId()).ifPresent(c -> dto.setIntendedClassName(c.getName()));
        }
        dto.setStatus(app.getStatus());
        dto.setExamScore(app.getExamScore());
        dto.setInterviewScore(app.getInterviewScore());
        dto.setReviewNotes(app.getReviewNotes());
        dto.setCreatedAt(app.getCreatedAt());
        dto.setMetadata(app.getMetadata());
        return dto;
    }
}
