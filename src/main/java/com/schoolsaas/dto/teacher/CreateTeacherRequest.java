package com.schoolsaas.dto.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,32}$",
             message = "Password must be 8-32 chars with uppercase, lowercase, number and special char (@$!%*?&)")
    private String password;

    private Map<String, Object> metadata;

    // Optional: subject-class assignments to create immediately
    private List<SubjectClassAssignment> subjectAssignments;

    @Data
    public static class SubjectClassAssignment {
        private UUID subjectId;
        private UUID classId;
    }
}
