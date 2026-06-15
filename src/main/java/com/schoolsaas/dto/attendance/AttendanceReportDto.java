package com.schoolsaas.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportDto {
    private long totalSchoolDays;
    private long totalStudents;
    private long presentCount;
    private long absentCount;
    private long lateCount;
    private long excusedCount;
    private BigDecimal averageAttendancePercentage;
    private List<StudentAttendanceReport> studentReports;
    private List<DailyAttendanceSummary> dailySummaries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAttendanceReport {
        private String studentId;
        private String studentName;
        private String admissionNumber;
        private long presentDays;
        private long absentDays;
        private long lateDays;
        private long excusedDays;
        private long totalDays;
        private BigDecimal attendancePercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAttendanceSummary {
        private String date;
        private long presentCount;
        private long absentCount;
        private long lateCount;
        private long excusedCount;
        private long totalCount;
    }
}
