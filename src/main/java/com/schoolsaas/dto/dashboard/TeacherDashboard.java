package com.schoolsaas.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboard {
    private TeacherInfo teacher;
    private List<ClassAssignment> myClasses;
    private int totalStudents;
    private int pendingContentApprovals;
    private List<RecentSubmission> recentSubmissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfo {
        private String id;
        private String fullName;
        private String employeeId;
        private String email;
        private String specialization;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassAssignment {
        private String classId;
        private String className;
        private String subjectId;
        private String subjectName;
        private boolean isClassTeacher;
        private int studentCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSubmission {
        private String contentId;
        private String title;
        private String status;
        private String submittedAt;
    }
}
