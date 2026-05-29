package com.schoolsaas.service;

import com.schoolsaas.dto.onboarding.OnboardingStepDto;
import com.schoolsaas.model.OnboardingStep;
import com.schoolsaas.model.UserOnboardingProgress;
import com.schoolsaas.repository.OnboardingStepRepository;
import com.schoolsaas.repository.UserOnboardingProgressRepository;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingStepRepository stepRepository;
    private final UserOnboardingProgressRepository progressRepository;

    public List<OnboardingStepDto> getStepsForPage(String targetPage, String role) {
        List<OnboardingStep> steps = stepRepository.findByTargetPageAndIsActiveTrueOrderByStepOrderAsc(targetPage);
        UUID userId = SecurityUtils.getCurrentUserId();
        List<UserOnboardingProgress> progress = progressRepository.findByUserId(userId);

        return steps.stream().map(step -> {
            OnboardingStepDto dto = new OnboardingStepDto();
            dto.setId(step.getId());
            dto.setStepKey(step.getStepKey());
            dto.setTargetPage(step.getTargetPage());
            dto.setTargetSelector(step.getTargetSelector());
            dto.setTitle(step.getTitle());
            dto.setContent(step.getContent());
            dto.setPosition(step.getPosition());
            dto.setStepOrder(step.getStepOrder());
            dto.setTargetRoles(step.getTargetRoles());
            dto.setIsCompleted(progress.stream().anyMatch(p -> p.getStepKey().equals(step.getStepKey()) && p.getIsCompleted()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void completeStep(String stepKey) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UserOnboardingProgress progress = progressRepository.findByUserIdAndStepKey(userId, stepKey)
                .orElse(UserOnboardingProgress.builder().userId(userId).stepKey(stepKey).build());
        progress.setIsCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    @Transactional
    public void resetOnboarding(UUID userId) {
        List<UserOnboardingProgress> progress = progressRepository.findByUserId(userId);
        progressRepository.deleteAll(progress);
    }
}
