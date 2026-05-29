package com.schoolsaas.dto.teacher;

import com.schoolsaas.model.Teacher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponse {
    private UUID id;
    private String employeeId;
    private String fullName;
    private String email;
    private String phone;
    private String specialization;
    private String qualification;
    private LocalDate dateOfJoining;
    private String status;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    public static TeacherResponse fromEntity(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .employeeId(teacher.getEmployeeId())
                .fullName(teacher.getFullName())
                .email(teacher.getEmail())
                .phone(teacher.getPhone())
                .specialization(teacher.getSpecialization())
                .qualification(teacher.getQualification())
                .dateOfJoining(teacher.getDateOfJoining())
                .status(teacher.getStatus())
                .metadata(teacher.getMetadata())
                .createdAt(teacher.getCreatedAt())
                .build();
    }
}
