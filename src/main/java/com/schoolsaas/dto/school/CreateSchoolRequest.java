package com.schoolsaas.dto.school;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class CreateSchoolRequest {
    @NotBlank(message = "School name is required")
    private String name;

    private String subdomain;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String address;
    private String logoUrl;
    private Map<String, Object> config;
}
