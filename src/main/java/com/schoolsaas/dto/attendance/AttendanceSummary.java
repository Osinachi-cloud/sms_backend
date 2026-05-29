package com.schoolsaas.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummary {
    private long totalDays;
    private long presentDays;
    private long absentDays;
    private long lateDays;
    private long excusedDays;
    private double attendancePercentage;
}
