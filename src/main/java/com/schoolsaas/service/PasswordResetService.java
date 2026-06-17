package com.schoolsaas.service;

import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.PasswordResetToken;
import com.schoolsaas.model.User;
import com.schoolsaas.repository.PasswordResetTokenRepository;
import com.schoolsaas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    // Default OTP for development/testing when email service is not ready
    private static final String DEFAULT_OTP = "12345";
    private static final int OTP_EXPIRY_MINUTES = 15;
    private static final boolean USE_DEFAULT_OTP = true; // Toggle this to switch to random OTPs

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String otp = generateOtp();

        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .build();

        tokenRepository.save(token);

        // TODO: Integrate with email service to send OTP to user's email
        // For now, log the OTP so developers can see it in console
        log.info("Password reset OTP for {}: {} (expires in {} minutes)", email, otp, OTP_EXPIRY_MINUTES);
    }

    @Transactional(readOnly = true)
    public void verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        PasswordResetToken token = tokenRepository
                .findTopByUserIdAndOtpOrderByCreatedAtDesc(user.getId(), otp)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (!token.isValid()) {
            throw new BadRequestException("OTP has expired or already been used");
        }
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        PasswordResetToken token = tokenRepository
                .findTopByUserIdAndOtpOrderByCreatedAtDesc(user.getId(), otp)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (!token.isValid()) {
            throw new BadRequestException("OTP has expired or already been used");
        }

        token.setUsed(true);
        tokenRepository.save(token);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", email);
    }

    private String generateOtp() {
        if (USE_DEFAULT_OTP) {
            return DEFAULT_OTP;
        }
        Random random = new Random();
        int otp = 10000 + random.nextInt(90000); // 5-digit number
        return String.valueOf(otp);
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now().minusHours(1));
        log.debug("Cleaned up expired password reset tokens");
    }
}
