package com.schoolsaas.dto.dashboard;

import com.schoolsaas.dto.attendance.AttendanceSummary;
import com.schoolsaas.dto.grade.GradeResponse;
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
public class StudentDashboard {
    private StudentInfo student;
    private ClassInfo currentClass;
    private List<SubjectWithGrade> subjects;
    private AttendanceSummary attendance;
    private FeeStatus feeStatus;
    private List<Assignment> upcomingAssignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        private String id;
        private String fullName;
        private String admissionNumber;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInfo {
        private String id;
        private String name;
        private String classTeacher;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectWithGrade {
        private String subjectId;
        private String subjectName;
        private String subjectCode;
        private String teacherName;
        private BigDecimal latestScore;
        private BigDecimal maxScore;
        private String gradeLetter;
        private String termName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeStatus {
        private BigDecimal totalDue;
        private BigDecimal totalPaid;
        private BigDecimal balance;
        private int pendingItems;
        private int overdueItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assignment {
        private String id;
        private String title;
        private String subjectName;
        private String dueDate;
        private String status;
    }
}
