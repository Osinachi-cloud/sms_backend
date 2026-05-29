package com.schoolsaas.dto.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
public class CreateStudentRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private UUID classId;
    private String parentName;

    @Email(message = "Invalid parent email format")
    private String parentEmail;

    private String parentPhone;
    private String admissionNumber;
    private Map<String, Object> metadata;
}
