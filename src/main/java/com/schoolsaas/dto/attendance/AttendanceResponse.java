package com.schoolsaas.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID classId;
    private String className;
    private LocalDate date;
    private String status;
    private String remarks;
}
