package com.schoolsaas.dto.onboarding;

import lombok.Data;

import java.util.UUID;

@Data
public class OnboardingStepDto {
    private UUID id;
    private String stepKey;
    private String targetPage;
    private String targetSelector;
    private String title;
    private String content;
    private String position;
    private Integer stepOrder;
    private String[] targetRoles;
    private Boolean isCompleted;
}
