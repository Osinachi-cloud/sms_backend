package com.schoolsaas.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class LoginRequest {
    @NotBlank(message = "Email or username is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private UUID schoolId;
}
