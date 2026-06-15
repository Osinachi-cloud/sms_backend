package com.schoolsaas.dto.school;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class UpdateSchoolRequest {
    private String name;
    private String subdomain;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String address;
    private String logoUrl;
    private Map<String, Object> config;

    // Admin fields
    private UUID adminUserId;
    private String adminFullName;

    @Email(message = "Invalid admin email format")
    private String adminEmail;

    private String adminPassword;
}
