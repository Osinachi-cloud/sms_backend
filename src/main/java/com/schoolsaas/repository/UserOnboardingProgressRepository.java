package com.schoolsaas.repository;

import com.schoolsaas.model.UserOnboardingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOnboardingProgressRepository extends JpaRepository<UserOnboardingProgress, UUID> {
    List<UserOnboardingProgress> findByUserId(UUID userId);
    Optional<UserOnboardingProgress> findByUserIdAndStepKey(UUID userId, String stepKey);
}
