package com.schoolsaas.controller;

import com.schoolsaas.dto.auth.AuthResponse;
import com.schoolsaas.dto.auth.ChangePasswordRequest;
import com.schoolsaas.dto.auth.LoginRequest;
import com.schoolsaas.dto.auth.RegisterRequest;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/switch-school")
    public ResponseEntity<AuthResponse> switchSchool(@RequestBody Map<String, String> body) {
        String schoolId = body.get("schoolId");
        return ResponseEntity.ok(authService.switchSchool(SecurityUtils.getCurrentUserId(), schoolId));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
