package com.schoolsaas.dto.attendance;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class MarkAttendanceRequest {
    private LocalDate date;
    private UUID classId;
    private List<StudentAttendanceRecord> records;

    @Data
    public static class StudentAttendanceRecord {
        private UUID studentId;
        private String status;
        private String remarks;
    }
}
