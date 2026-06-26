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
    private final NotificationService notificationService;

    private final GradebookService gradebookService;

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentReport(UUID schoolId, UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        List<Subject> subjects = subjectRepository.findBySchoolId(schoolId);
        List<Map<String, Object>> subjectBreakdowns = new ArrayList<>();
        double totalAverage = 0;
        int count = 0;

        for (Subject subject : subjects) {
            // Compute gradebook for the class and subject, then filter for student
            // Optimization: Filter at service level if needed
            Map<String, Object> gradebook = gradebookService.computeGradebook(schoolId, student.getClassId(), subject.getId());
            List<Map<String, Object>> studentResults = (List<Map<String, Object>>) gradebook.get("students");
            
            Optional<Map<String, Object>> studentRow = studentResults.stream()
                    .filter(r -> r.get("student_id").equals(studentId))
                    .findFirst();

            if (studentRow.isPresent()) {
                Map<String, Object> row = studentRow.get();
                row.put("subject_name", subject.getName());
                row.put("grading_scheme", gradebook.get("grading_scheme"));
                subjectBreakdowns.add(row);
                
                if (row.get("total") != null) {
                    totalAverage += (double) row.get("total");
                    count++;
                }
            }
        }

        Double overallAverage = count > 0 ? Math.round((totalAverage / count) * 10.0) / 10.0 : null;
        Map<String, Object> report = new HashMap<>();
        report.put("student_name", student.getFullName());
        report.put("class_id", student.getClassId());
        report.put("subjects", subjectBreakdowns);
        report.put("overall_average", overallAverage);
        report.put("overall_grade", overallAverage != null ? calculateGradeLetter(BigDecimal.valueOf(overallAverage)) : null);
        
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

    private String calculateGradeLetter(BigDecimal score) {
        if (score == null) return "F";
        double s = score.doubleValue();
        if (s >= 70) return "A";
        if (s >= 60) return "B";
        if (s >= 50) return "C";
        if (s >= 45) return "D";
        if (s >= 40) return "E";
        return "F";
    }
}
