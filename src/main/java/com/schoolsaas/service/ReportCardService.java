package com.schoolsaas.service;

import com.schoolsaas.dto.reportcard.ReportCardDto;
import com.schoolsaas.dto.reportcard.ReportCardEntryDto;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportCardService {

    private final ReportCardRepository reportCardRepository;
    private final ReportCardEntryRepository entryRepository;
    private final ReportCardTemplateRepository templateRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;
    private final TermRepository termRepository;
    private final AcademicSessionRepository sessionRepository;
    private final StudentAffectiveRatingRepository affectiveRatingRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ReportCardPdfService pdfService;

    private final GradebookService gradebookService;

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentReport(UUID schoolId, UUID studentId, UUID termId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // School info
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        // Class info
        String className = null;
        String classTeacherName = null;
        if (student.getClassId() != null) {
            Optional<SchoolClass> schoolClassOpt = classRepository.findById(student.getClassId());
            if (schoolClassOpt.isPresent()) {
                SchoolClass schoolClass = schoolClassOpt.get();
                className = schoolClass.getName();
            }
        }

        // Term info
        String termName = null;
        String sessionName = null;
        if (termId != null) {
            Optional<Term> termOpt = termRepository.findById(termId);
            if (termOpt.isPresent()) {
                Term term = termOpt.get();
                termName = term.getName();
                if (term.getSessionId() != null) {
                    Optional<AcademicSession> sessionOpt = sessionRepository.findById(term.getSessionId());
                    sessionName = sessionOpt.map(AcademicSession::getName).orElse(null);
                }
            }
        }

        // Attendance summary
        List<Attendance> attendances = attendanceRepository.findByStudentIdOrderByDateDesc(studentId);
        int presentCount = (int) attendances.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        int absentCount = (int) attendances.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        int lateCount = (int) attendances.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        int totalAttendanceDays = presentCount + absentCount + lateCount;

        // Build academic breakdowns from gradebook
        List<Subject> subjects = subjectRepository.findBySchoolId(schoolId);
        List<Map<String, Object>> subjectBreakdowns = new ArrayList<>();
        double totalAverage = 0;
        int count = 0;

        for (Subject subject : subjects) {
            if (student.getClassId() == null) continue;
            try {
                Map<String, Object> gradebook = gradebookService.computeGradebook(schoolId, student.getClassId(), subject.getId());
                List<Map<String, Object>> studentResults = (List<Map<String, Object>>) gradebook.get("students");
                if (studentResults == null) continue;

                Optional<Map<String, Object>> studentRow = studentResults.stream()
                        .filter(r -> studentId.toString().equals(String.valueOf(r.get("student_id"))))
                        .findFirst();

                if (studentRow.isPresent()) {
                    Map<String, Object> row = studentRow.get();
                    Map<String, Object> subjectData = new HashMap<>();
                    subjectData.put("subject_name", subject.getName());
                    subjectData.put("subject_id", subject.getId());
                    subjectData.put("components", row.get("components"));
                    subjectData.put("total", row.get("total"));
                    subjectData.put("grading_scheme", gradebook.get("grading_scheme"));
                    subjectBreakdowns.add(subjectData);

                    if (row.get("total") != null) {
                        totalAverage += ((Number) row.get("total")).doubleValue();
                        count++;
                    }
                }
            } catch (Exception ex) {
                // Log and continue with next subject instead of crashing entire report
                System.err.println("Error computing gradebook for subject " + subject.getId() + ": " + ex.getMessage());
            }
        }

        Double overallAverage = count > 0 ? Math.round((totalAverage / count) * 10.0) / 10.0 : null;

        // Affective domain (behavioral) - read from dedicated ratings table
        // Student metadata for comments
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = student.getMetadata() != null ? student.getMetadata() : new HashMap<>();

        // Affective domain (behavioral) - read from dedicated ratings table
        List<StudentAffectiveRating> affectiveRatings = affectiveRatingRepository
                .findBySchoolIdAndStudentIdAndTermId(schoolId, studentId, termId != null ? termId : UUID.fromString("00000000-0000-0000-0000-000000000000"));

        // If no term-specific ratings found and termId is provided, try without strict term filter via fallback
        if ((affectiveRatings == null || affectiveRatings.isEmpty()) && termId != null) {
            affectiveRatings = affectiveRatingRepository.findBySchoolIdAndStudentIdAndTermId(schoolId, studentId, termId);
        }

        Map<String, Integer> ratingMap = new HashMap<>();
        if (affectiveRatings != null) {
            for (StudentAffectiveRating r : affectiveRatings) {
                ratingMap.put(r.getTrait(), r.getRating());
            }
        }

        List<Map<String, Object>> affectiveData = new ArrayList<>();
        String[] affectiveTraits = {"Punctuality", "Neatness", "Politeness", "Attentiveness",
                "Obedience", "Hardwork", "Self Control", "Honesty", "Leadership", "Sportsmanship"};
        for (String trait : affectiveTraits) {
            Map<String, Object> traitMap = new HashMap<>();
            traitMap.put("trait", trait);
            traitMap.put("rating", ratingMap.getOrDefault(trait, null));
            affectiveData.add(traitMap);
        }

        Map<String, Object> report = new HashMap<>();

        // Build grading scale from school config or use defaults
        List<Map<String, Object>> gradingScale = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> configuredScale = (school.getConfig() != null && school.getConfig().get("gradingScale") instanceof List)
                ? (List<Map<String, Object>>) school.getConfig().get("gradingScale")
                : null;

        if (configuredScale != null && !configuredScale.isEmpty()) {
            for (Map<String, Object> entry : configuredScale) {
                Map<String, Object> scaleMap = new HashMap<>();
                scaleMap.put("grade", entry.get("grade"));
                scaleMap.put("minScore", entry.get("minScore"));
                scaleMap.put("maxScore", entry.get("maxScore"));
                scaleMap.put("label", entry.get("label"));
                gradingScale.add(scaleMap);
            }
        } else {
            // Default grading scale
            gradingScale.add(Map.of("grade", "A", "minScore", 70, "maxScore", 100, "label", "Excellent"));
            gradingScale.add(Map.of("grade", "B", "minScore", 60, "maxScore", 69, "label", "Very Good"));
            gradingScale.add(Map.of("grade", "C", "minScore", 50, "maxScore", 59, "label", "Good"));
            gradingScale.add(Map.of("grade", "D", "minScore", 45, "maxScore", 49, "label", "Fair"));
            gradingScale.add(Map.of("grade", "F", "minScore", 0, "maxScore", 44, "label", "Poor"));
        }

        Map<String, Object> schoolMap = new HashMap<>();
        schoolMap.put("name", school.getName());
        schoolMap.put("address", school.getAddress());
        schoolMap.put("phone", school.getPhone());
        schoolMap.put("email", school.getEmail());
        schoolMap.put("logoUrl", school.getLogoUrl());
        Map<String, Object> cfg = school.getConfig() != null ? school.getConfig() : new HashMap<>();
        schoolMap.put("primaryColor", cfg.getOrDefault("primaryColor", "#3b82f6"));
        schoolMap.put("secondaryColor", cfg.getOrDefault("secondaryColor", "#8b5cf6"));
        schoolMap.put("accentColor", cfg.getOrDefault("accentColor", "#10b981"));
        report.put("school", schoolMap);

        Map<String, Object> studentMap = new HashMap<>();
        studentMap.put("id", student.getId());
        studentMap.put("name", student.getFullName());
        studentMap.put("admission_number", student.getAdmissionNumber());
        studentMap.put("gender", student.getGender());
        studentMap.put("date_of_birth", student.getDateOfBirth());
        studentMap.put("class_id", student.getClassId());
        studentMap.put("class_name", className);
        studentMap.put("class_teacher_name", classTeacherName);
        report.put("student", studentMap);

        Map<String, Object> termMap = new HashMap<>();
        termMap.put("id", termId);
        termMap.put("name", termName);
        termMap.put("session_name", sessionName);
        report.put("term", termMap);

        Map<String, Object> attendanceMap = new HashMap<>();
        attendanceMap.put("present", presentCount);
        attendanceMap.put("absent", absentCount);
        attendanceMap.put("late", lateCount);
        attendanceMap.put("total_days", totalAttendanceDays);
        attendanceMap.put("percentage", totalAttendanceDays > 0 ? Math.round((presentCount * 100.0 / totalAttendanceDays) * 10.0) / 10.0 : 0);
        report.put("attendance", attendanceMap);
        // Compute overall grade using configured grading scale if available
        String overallGradeLetter = null;
        if (overallAverage != null) {
            overallGradeLetter = calculateGradeLetter(BigDecimal.valueOf(overallAverage), gradingScale);
        }

        report.put("subjects", subjectBreakdowns);
        report.put("overall_average", overallAverage);
        report.put("overall_grade", overallGradeLetter);
        report.put("grading_scale", gradingScale);
        report.put("affective_domain", affectiveData);
        report.put("teacher_comment", metadata.getOrDefault("teacherComment", ""));
        report.put("principal_comment", metadata.getOrDefault("principalComment", ""));

        return report;
    }

    @Transactional
    public ReportCardDto generateReportCard(UUID schoolId, UUID studentId, UUID termId, UUID templateId) {
        Student student = studentRepository.findById(studentId).orElseThrow();
        ReportCardTemplate template = templateRepository.findById(templateId).orElseThrow();

        ReportCard rc = ReportCard.builder()
                .schoolId(schoolId)
                .studentId(studentId)
                .termId(termId)
                .templateId(templateId)
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();

        // Calculate attendance
        List<Attendance> attendances = attendanceRepository.findByStudentIdOrderByDateDesc(studentId);
        rc.setAttendancePresent((int) attendances.stream().filter(a -> "PRESENT".equals(a.getStatus())).count());
        rc.setAttendanceAbsent((int) attendances.stream().filter(a -> "ABSENT".equals(a.getStatus())).count());
        rc.setAttendanceLate((int) attendances.stream().filter(a -> "LATE".equals(a.getStatus())).count());

        rc = reportCardRepository.save(rc);

        // Pull grades for the term
        List<Grade> grades = gradeRepository.findByStudentIdAndTermId(studentId, termId);
        BigDecimal totalScore = BigDecimal.ZERO;
        int count = 0;

        for (Grade grade : grades) {
            ReportCardEntry entry = ReportCardEntry.builder()
                    .reportCardId(rc.getId())
                    .subjectId(grade.getSubjectId())
                    .testScore(null) // Or some logic if needed
                    .examScore(grade.getScore())
                    .totalScore(grade.getScore())
                    .gradeLetter(grade.getGradeLetter())
                    .remarks(grade.getRemarks())
                    .teacherId(grade.getEnteredBy())
                    .build();
            entryRepository.save(entry);
            if (grade.getScore() != null) {
                totalScore = totalScore.add(grade.getScore());
                count++;
            }
        }

        if (count > 0) {
            rc.setTotalScore(totalScore);
            rc.setAverageScore(totalScore.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP));
            rc.setOverallGrade(calculateGradeLetter(rc.getAverageScore()));
        }
        rc = reportCardRepository.save(rc);

        return mapToDto(rc);
    }

    @Transactional
    public ReportCardDto publishReportCard(UUID reportCardId, String teacherComment, String principalComment) {
        ReportCard rc = reportCardRepository.findById(reportCardId).orElseThrow();
        rc.setTeacherComment(teacherComment);
        rc.setPrincipalComment(principalComment);
        rc.setStatus("PUBLISHED");
        rc.setPublishedAt(LocalDateTime.now());
        rc = reportCardRepository.save(rc);

        Student student = studentRepository.findById(rc.getStudentId()).orElse(null);
        if (student != null && student.getUserId() != null) {
            notificationService.sendNotification(student.getUserId(), rc.getSchoolId(), "Report Card Published",
                    "Your report card is now available.", "GRADE", rc.getId());
        }

        return mapToDto(rc);
    }

    public Page<ReportCardDto> listReportCards(UUID schoolId, Pageable pageable) {
        return reportCardRepository.findBySchoolId(schoolId, pageable).map(this::mapToDto);
    }

    public List<ReportCardDto> getStudentReportCards(UUID studentId) {
        return reportCardRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    private ReportCardDto mapToDto(ReportCard rc) {
        ReportCardDto dto = new ReportCardDto();
        dto.setId(rc.getId());
        dto.setStudentId(rc.getStudentId());
        Student student = studentRepository.findById(rc.getStudentId()).orElse(null);
        if (student != null) {
            dto.setStudentName(student.getFullName());
            dto.setAdmissionNumber(student.getAdmissionNumber());
        }
        dto.setTermId(rc.getTermId());
        dto.setAttendancePresent(rc.getAttendancePresent());
        dto.setAttendanceAbsent(rc.getAttendanceAbsent());
        dto.setAttendanceLate(rc.getAttendanceLate());
        dto.setTotalScore(rc.getTotalScore());
        dto.setAverageScore(rc.getAverageScore());
        dto.setOverallGrade(rc.getOverallGrade());
        dto.setClassPosition(rc.getClassPosition());
        dto.setClassSize(rc.getClassSize());
        dto.setTeacherComment(rc.getTeacherComment());
        dto.setPrincipalComment(rc.getPrincipalComment());
        dto.setStatus(rc.getStatus());
        dto.setGeneratedPdfUrl(rc.getGeneratedPdfUrl());
        dto.setPublishedAt(rc.getPublishedAt());
        dto.setEntries(entryRepository.findByReportCardId(rc.getId()).stream().map(e -> {
            ReportCardEntryDto ed = new ReportCardEntryDto();
            ed.setSubjectId(e.getSubjectId());
            if (e.getSubjectId() != null) {
                subjectRepository.findById(e.getSubjectId()).ifPresent(s -> ed.setSubjectName(s.getName()));
            }
            ed.setTestScore(e.getTestScore());
            ed.setExamScore(e.getExamScore());
            ed.setTotalScore(e.getTotalScore());
            ed.setGradeLetter(e.getGradeLetter());
            ed.setRemarks(e.getRemarks());
            if (e.getTeacherId() != null) {
                teacherRepository.findById(e.getTeacherId()).ifPresent(t -> ed.setTeacherName(t.getFullName()));
            }
            return ed;
        }).collect(Collectors.toList()));
        return dto;
    }

    private String calculateGradeLetter(BigDecimal score, List<Map<String, Object>> gradingScale) {
        if (score == null) return "F";
        double s = score.doubleValue();
        if (gradingScale != null) {
            // Sort by minScore descending so higher grades are checked first
            List<Map<String, Object>> sorted = gradingScale.stream()
                    .sorted((a, b) -> {
                        int minA = a.get("minScore") instanceof Number ? ((Number) a.get("minScore")).intValue() : 0;
                        int minB = b.get("minScore") instanceof Number ? ((Number) b.get("minScore")).intValue() : 0;
                        return Integer.compare(minB, minA);
                    })
                    .toList();
            for (Map<String, Object> entry : sorted) {
                int min = entry.get("minScore") instanceof Number ? ((Number) entry.get("minScore")).intValue() : 0;
                int max = entry.get("maxScore") instanceof Number ? ((Number) entry.get("maxScore")).intValue() : 100;
                if (s >= min && s <= max) {
                    return String.valueOf(entry.get("grade"));
                }
            }
        }
        // Fallback default
        if (s >= 70) return "A";
        if (s >= 60) return "B";
        if (s >= 50) return "C";
        if (s >= 45) return "D";
        if (s >= 40) return "E";
        return "F";
    }

    private String calculateGradeLetter(BigDecimal score) {
        return calculateGradeLetter(score, null);
    }
}
