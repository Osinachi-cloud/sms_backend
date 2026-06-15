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
public class ClassAttendanceDto {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private UUID classId;
    private String className;
    private LocalDate date;
    private String status;
    private String remarks;
    private UUID markedBy;
    private String markedByName;
}
