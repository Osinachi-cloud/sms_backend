package com.schoolsaas.dto.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class CreateTeacherRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String employeeId;
    private String specialization;
    private String qualification;
    private LocalDate dateOfJoining;
    private String password;
    private Map<String, Object> metadata;
}
