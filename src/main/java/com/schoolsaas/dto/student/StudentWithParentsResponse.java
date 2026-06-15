package com.schoolsaas.dto.student;

import com.schoolsaas.dto.parent.ParentDto;
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
public class StudentWithParentsResponse {
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
    private String parentName;         // legacy single field
    private String parentEmail;        // legacy single field
    private String parentPhone;        // legacy single field
    private LocalDate admissionDate;
    private String status;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private List<ParentDto> parents;   // full parent details
}
