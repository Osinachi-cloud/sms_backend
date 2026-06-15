package com.schoolsaas.dto.student;

import com.schoolsaas.dto.attendance.AttendanceSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailResponse {
    private UUID id;
    private String admissionNumber;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private UUID classId;
    private String className;
    private LocalDate admissionDate;
    private String status;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    // Legacy denormalized parent fields (backward compatibility)
    private String parentName;
    private String parentEmail;
    private String parentPhone;

    // View context
    private Boolean limitedView;

    // Nested data
    private List<ParentInfo> parents;
    private AttendanceSummary attendanceSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentInfo {
        private UUID id;
        private String fullName;
        private String email;
        private String phone;
        private String relationship;
        private String address;
        private String occupation;
        private Boolean isPrimary;
    }
}
