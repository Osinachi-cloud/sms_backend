package com.schoolsaas.dto.idcard;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class StudentIdCardDto {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentClass;
    private String admissionNumber;
    private String cardNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String qrCode;
    private String generatedPdfUrl;
    private String status;
}
