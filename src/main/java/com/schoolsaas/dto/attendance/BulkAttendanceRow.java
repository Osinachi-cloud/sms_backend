package com.schoolsaas.dto.attendance;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class BulkAttendanceRow {
    private LocalDate date;
    private UUID studentId;
    private String studentEmail;
    private String admissionNumber;
    private String status;
    private String remarks;
}
