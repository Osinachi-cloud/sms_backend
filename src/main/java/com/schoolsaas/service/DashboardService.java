package com.schoolsaas.service;

import com.schoolsaas.dto.attendance.AttendanceSummary;
import com.schoolsaas.dto.dashboard.DashboardStats;
import com.schoolsaas.dto.dashboard.StudentDashboard;
import com.schoolsaas.dto.dashboard.TeacherDashboard;
import com.schoolsaas.dto.grade.GradeResponse;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    public DashboardStats getSchoolDashboardStats(UUID schoolId) {
        long totalStudents = studentRepository.countBySchoolId(schoolId);
        long activeStudents = studentRepository.countBySchoolIdAndStatus(schoolId, "ACTIVE");
        long totalTeachers = teacherRepository.countBySchoolId(schoolId);
        long activeTeachers = teacherRepository.countBySchoolIdAndStatus(schoolId, "ACTIVE");
        long totalClasses = classRepository.countBySchoolId(schoolId);

        BigDecimal totalRevenue = paymentRepository.sumAmountBySchoolIdAndStatus(schoolId, "SUCCESS");
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        long pendingApprovals = contentItemRepository.countBySchoolIdAndStatus(schoolId, "PENDING");

        List<DashboardStats.RecentActivity> activities = List.of(
                DashboardStats.RecentActivity.builder()
                        .action("New student enrolled")
                        .user("Admin")
                        .time("2 hours ago")
                        .type("ENROLLMENT")
                        .build(),
                DashboardStats.RecentActivity.builder()
                        .action("Payment received")
                        .user("Parent")
                        .time("3 hours ago")
                        .type("PAYMENT")
                        .build()
        );

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

        List<TeacherClass> assignments = teacherClassRepository.findByTeacherId(teacherId);

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
}
