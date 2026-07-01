package com.schoolsaas.dto.student;

import com.schoolsaas.model.Student;
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
public class StudentResponse {
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
    private String parentName;
    private String parentEmail;
    private String parentPhone;
    private String photoUrl;
    private LocalDate admissionDate;
    private String status;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private Boolean isClassTeacher;

    public static StudentResponse fromEntity(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .admissionNumber(student.getAdmissionNumber())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .classId(student.getClassId())
                .className(student.getSchoolClass() != null ? student.getSchoolClass().getName() : null)
                .parentName(student.getParentName())
                .parentEmail(student.getParentEmail())
                .parentPhone(student.getParentPhone())
                .photoUrl(student.getPhotoUrl())
                .admissionDate(student.getAdmissionDate())
                .status(student.getStatus())
                .metadata(student.getMetadata())
                .createdAt(student.getCreatedAt())
                .build();
    }
}
