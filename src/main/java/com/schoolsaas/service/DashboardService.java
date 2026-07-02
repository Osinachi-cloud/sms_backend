package com.schoolsaas.service;

import com.schoolsaas.dto.attendance.AttendanceSummary;
import com.schoolsaas.dto.dashboard.DashboardStats;
import com.schoolsaas.model.StudentSubjectEnrollment;
import com.schoolsaas.repository.StudentSubjectEnrollmentRepository;
import com.schoolsaas.dto.dashboard.StudentDashboard;
import com.schoolsaas.dto.dashboard.TeacherDashboard;
import com.schoolsaas.dto.grade.GradeResponse;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final PaymentRepository paymentRepository;
    private final ContentItemRepository contentItemRepository;
    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final TermRepository termRepository;
    private final AttendanceService attendanceService;
    private final GradeService gradeService;
    private final TeacherClassRepository teacherClassRepository;

    private final StudentSubjectEnrollmentRepository studentSubjectEnrollmentRepository;

    public DashboardStats getSchoolDashboardStats(UUID schoolId) {
        long totalStudents = studentRepository.countBySchoolId(schoolId);
        long activeStudents = studentRepository.countBySchoolIdAndStatus(schoolId, "ACTIVE");
        long totalTeachers = teacherRepository.countBySchoolId(schoolId);
        long activeTeachers = teacherRepository.countBySchoolIdAndStatus(schoolId, "ACTIVE");
        long totalClasses = classRepository.countBySchoolId(schoolId);

        BigDecimal totalRevenue = paymentRepository.sumAmountBySchoolIdAndStatus(schoolId, "SUCCESS");
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        long pendingApprovals = contentItemRepository.countBySchoolIdAndStatus(schoolId, "PENDING");

        // Build real recent activities from students, payments, and content
        List<DashboardStats.RecentActivity> activities = buildRecentActivities(schoolId);

        return DashboardStats.builder()
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .totalTeachers(totalTeachers)
                .activeTeachers(activeTeachers)
                .totalClasses(totalClasses)
                .totalRevenue(totalRevenue)
                .pendingFees(BigDecimal.ZERO)
                .pendingContentApprovals(pendingApprovals)
                .recentActivities(activities)
                .build();
    }

    public StudentDashboard getStudentDashboard(UUID schoolId, UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        StudentDashboard.StudentInfo studentInfo = StudentDashboard.StudentInfo.builder()
                .id(student.getId().toString())
                .fullName(student.getFullName())
                .admissionNumber(student.getAdmissionNumber())
                .email(student.getEmail())
                .build();

        SchoolClass schoolClass = student.getClassId() != null
                ? classRepository.findById(student.getClassId()).orElse(null)
                : null;

        StudentDashboard.ClassInfo classInfo = null;
        if (schoolClass != null) {
            classInfo = StudentDashboard.ClassInfo.builder()
                    .id(schoolClass.getId().toString())
                    .name(schoolClass.getName())
                    .build();
        }

        List<GradeResponse> grades = gradeService.getStudentGrades(schoolId, studentId);

        Map<UUID, GradeResponse> latestGradesBySubject = new HashMap<>();
        for (GradeResponse grade : grades) {
            if (grade.getSubjectId() != null && "EXAM".equals(grade.getAssessmentType())) {
                latestGradesBySubject.putIfAbsent(grade.getSubjectId(), grade);
            }
        }

        List<StudentDashboard.SubjectWithGrade> subjects = latestGradesBySubject.values().stream()
                .map(g -> StudentDashboard.SubjectWithGrade.builder()
                        .subjectId(g.getSubjectId().toString())
                        .subjectName(g.getSubjectName())
                        .subjectCode(g.getSubjectCode())
                        .latestScore(g.getScore())
                        .maxScore(g.getMaxScore())
                        .gradeLetter(g.getGradeLetter())
                        .termName(g.getTermName())
                        .build())
                .collect(Collectors.toList());

        AttendanceSummary attendance = attendanceService.getStudentAttendanceSummary(schoolId, studentId);

        // Ensure all relevant subjects are shown, even if no grades exist
        // 1. Get subjects from explicit enrollments
        List<StudentSubjectEnrollment> enrollments = studentSubjectEnrollmentRepository.findBySchoolIdAndStudentId(schoolId, studentId);
        Set<UUID> subjectIds = enrollments.stream()
                .map(StudentSubjectEnrollment::getSubjectId)
                .collect(Collectors.toSet());

        // 2. Get subjects assigned to the student's class (fallback/additional)
        if (student.getClassId() != null) {
            List<Subject> classSubjects = subjectRepository.findBySchoolIdAndClassId(schoolId, student.getClassId());
            for (Subject s : classSubjects) {
                subjectIds.add(s.getId());
            }
        }

        Map<UUID, Subject> schoolSubjects = subjectRepository.findBySchoolId(schoolId)
                .stream().collect(Collectors.toMap(Subject::getId, s -> s));

        // Use a Set of subject IDs already added from grades to avoid duplicates
        Set<UUID> addedSubjectIds = new HashSet<>(latestGradesBySubject.keySet());

        for (UUID subId : subjectIds) {
            if (!addedSubjectIds.contains(subId)) {
                Subject subj = schoolSubjects.get(subId);
                if (subj != null) {
                    subjects.add(StudentDashboard.SubjectWithGrade.builder()
                            .subjectId(subj.getId().toString())
                            .subjectName(subj.getName())
                            .subjectCode(subj.getCode())
                            .latestScore(null)
                            .maxScore(BigDecimal.valueOf(100))
                            .gradeLetter(null)
                            .termName("Current Term")
                            .build());
                    addedSubjectIds.add(subId);
                }
            }
        }

        StudentDashboard.FeeStatus feeStatus = StudentDashboard.FeeStatus.builder()
                .totalDue(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .pendingItems(0)
                .overdueItems(0)
                .build();

        return StudentDashboard.builder()
                .student(studentInfo)
                .currentClass(classInfo)
                .subjects(subjects)
                .attendance(attendance)
                .feeStatus(feeStatus)
                .upcomingAssignments(List.of())
                .build();
    }

    public TeacherDashboard getTeacherDashboard(UUID schoolId, UUID teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        TeacherDashboard.TeacherInfo teacherInfo = TeacherDashboard.TeacherInfo.builder()
                .id(teacher.getId().toString())
                .fullName(teacher.getFullName())
                .employeeId(teacher.getEmployeeId())
                .email(teacher.getEmail())
                .specialization(teacher.getSpecialization())
                .build();

        List<TeacherClass> assignments = teacherClassRepository.findBySchoolIdAndTeacherId(schoolId, teacherId);

        Map<UUID, SchoolClass> classMap = classRepository.findBySchoolId(schoolId)
                .stream().collect(Collectors.toMap(SchoolClass::getId, c -> c));

        Map<UUID, Subject> subjectMap = subjectRepository.findBySchoolId(schoolId)
                .stream().collect(Collectors.toMap(Subject::getId, s -> s));

        List<TeacherDashboard.ClassAssignment> myClasses = assignments.stream()
                .map(tc -> {
                    SchoolClass sc = classMap.get(tc.getClassId());
                    Subject subj = tc.getSubjectId() != null ? subjectMap.get(tc.getSubjectId()) : null;

                    long studentCount = sc != null
                            ? studentRepository.countByClassId(sc.getId())
                            : 0;

                    return TeacherDashboard.ClassAssignment.builder()
                            .classId(tc.getClassId().toString())
                            .className(sc != null ? sc.getName() : "Unknown")
                            .subjectId(tc.getSubjectId() != null ? tc.getSubjectId().toString() : null)
                            .subjectName(subj != null ? subj.getName() : null)
                            .isClassTeacher(tc.getIsClassTeacher() != null && tc.getIsClassTeacher())
                            .studentCount((int) studentCount)
                            .build();
                })
                .collect(Collectors.toList());

        int totalStudents = myClasses.stream().mapToInt(TeacherDashboard.ClassAssignment::getStudentCount).sum();

        return TeacherDashboard.builder()
                .teacher(teacherInfo)
                .myClasses(myClasses)
                .totalStudents(totalStudents)
                .pendingContentApprovals(0)
                .recentSubmissions(List.of())
                .build();
    }

    private List<DashboardStats.RecentActivity> buildRecentActivities(UUID schoolId) {
        java.util.List<Student> recentStudents = studentRepository.findTop5BySchoolIdOrderByCreatedAtDesc(schoolId);
        java.util.List<Payment> recentPayments = paymentRepository.findTop5BySchoolIdOrderByCreatedAtDesc(schoolId);
        java.util.List<ContentItem> recentContent = contentItemRepository.findTop5BySchoolIdOrderByCreatedAtDesc(schoolId);

        Stream<DashboardStats.RecentActivity> studentStream = recentStudents.stream()
                .map(s -> DashboardStats.RecentActivity.builder()
                        .action("New student enrolled: " + s.getFullName())
                        .user("Admin")
                        .time(formatRelativeTime(s.getCreatedAt()))
                        .type("ENROLLMENT")
                        .build());

        Stream<DashboardStats.RecentActivity> paymentStream = recentPayments.stream()
                .map(p -> DashboardStats.RecentActivity.builder()
                        .action("Payment received: ₦" + p.getAmount() + (p.getStatus().equals("SUCCESS") ? "" : " (" + p.getStatus() + ")"))
                        .user("Parent")
                        .time(formatRelativeTime(p.getCreatedAt()))
                        .type("PAYMENT")
                        .build());

        Stream<DashboardStats.RecentActivity> contentStream = recentContent.stream()
                .map(c -> DashboardStats.RecentActivity.builder()
                        .action("New content added: " + c.getTitle())
                        .user("Teacher")
                        .time(formatRelativeTime(c.getCreatedAt()))
                        .type("CONTENT")
                        .build());

        return Stream.of(studentStream, paymentStream, contentStream)
                .flatMap(s -> s)
                .sorted((a, b) -> parseRelativeTime(b.getTime()).compareTo(parseRelativeTime(a.getTime())))
                .limit(6)
                .collect(Collectors.toList());
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Just now";
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        if (hours < 24) return hours + " hours ago";
        if (days < 7) return days + " days ago";
        if (days < 30) return (days / 7) + " weeks ago";
        return (days / 30) + " months ago";
    }

    private LocalDateTime parseRelativeTime(String time) {
        // Crude reverse parser for sorting — we just need ordering
        LocalDateTime now = LocalDateTime.now();
        if (time.contains("Just now")) return now;
        if (time.contains("minutes ago")) {
            int m = Integer.parseInt(time.replaceAll("\\D", ""));
            return now.minusMinutes(m);
        }
        if (time.contains("hours ago")) {
            int h = Integer.parseInt(time.replaceAll("\\D", ""));
            return now.minusHours(h);
        }
        if (time.contains("days ago")) {
            int d = Integer.parseInt(time.replaceAll("\\D", ""));
            return now.minusDays(d);
        }
        if (time.contains("weeks ago")) {
            int w = Integer.parseInt(time.replaceAll("\\D", ""));
            return now.minusWeeks(w);
        }
        if (time.contains("months ago")) {
            int mo = Integer.parseInt(time.replaceAll("\\D", ""));
            return now.minusMonths(mo);
        }
        return now;
    }
}
