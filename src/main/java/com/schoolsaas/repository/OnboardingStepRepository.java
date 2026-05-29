package com.schoolsaas.repository;

import com.schoolsaas.model.OnboardingStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnboardingStepRepository extends JpaRepository<OnboardingStep, java.util.UUID> {
    List<OnboardingStep> findByTargetPageAndIsActiveTrueOrderByStepOrderAsc(String targetPage);
    Optional<OnboardingStep> findByStepKey(String stepKey);
}
