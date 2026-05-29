package com.schoolsaas.dto.parent;

import lombok.Data;

import java.util.UUID;

@Data
public class ParentStudentInfo {
    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private String studentClass;
    private Boolean isPrimary;
}
