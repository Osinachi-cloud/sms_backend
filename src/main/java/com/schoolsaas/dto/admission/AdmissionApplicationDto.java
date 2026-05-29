package com.schoolsaas.dto.admission;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class AdmissionApplicationDto {
    private UUID id;
    private String applicationNumber;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String email;
    private String phone;
    private String address;
    private String previousSchool;
    private String lastClassCompleted;
    private String guardianName;
    private String guardianEmail;
    private String guardianPhone;
    private String guardianRelationship;
    private UUID intendedClassId;
    private String intendedClassName;
    private String status;
    private Double examScore;
    private Double interviewScore;
    private String reviewNotes;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;
}
