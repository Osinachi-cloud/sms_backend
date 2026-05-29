package com.schoolsaas.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private long totalStudents;
    private long activeStudents;
    private long totalTeachers;
    private long activeTeachers;
    private long totalClasses;
    private BigDecimal totalRevenue;
    private BigDecimal pendingFees;
    private long pendingContentApprovals;
    private List<RecentActivity> recentActivities;
    private List<RevenueData> revenueByMonth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String action;
        private String user;
        private String time;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueData {
        private String month;
        private BigDecimal amount;
    }
}
