package com.schoolsaas.dto.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^(?=.*[@$!%*?&._-])[A-Za-z0-9@$!%*?&._-]+$",
             message = "Username must contain at least one special character (@$!%*?&._-) and no spaces")
    private String username;

    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private UUID classId;
    private String parentName;

    @Email(message = "Invalid parent email format")
    private String parentEmail;

    private String parentPhone;
    private String photoUrl;
    private String admissionNumber;

    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,32}$",
             message = "Password must be 8-32 chars with uppercase, lowercase, number and special char (@$!%*?&)")
    private String password;

    private Map<String, Object> metadata;

    // Nested parent payload for creating/linking a parent
    private ParentPayload parent;

    @Data
    public static class ParentPayload {
        private UUID parentId; // for existing parent
        private String fullName;
        @Email(message = "Invalid parent email")
        private String email;
        private String phone;
        private String relationship;
        private String address;
        private String occupation;
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,32}$",
                 message = "Password must be 8-32 chars with uppercase, lowercase, number and special char (@$!%*?&)")
        private String password;
        private String status;
    }
}
