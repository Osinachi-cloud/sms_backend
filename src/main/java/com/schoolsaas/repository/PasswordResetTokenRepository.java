package com.schoolsaas.repository;

import com.schoolsaas.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findTopByUserIdAndOtpOrderByCreatedAtDesc(UUID userId, String otp);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
