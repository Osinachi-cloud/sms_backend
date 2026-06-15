package com.schoolsaas.service;

import com.schoolsaas.dto.attendance.*;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Attendance;
import com.schoolsaas.model.ParentStudent;
import com.schoolsaas.model.Student;
import com.schoolsaas.model.User;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final HolidayRepository holidayRepository;

    // ===========================
    // MARK / EDIT ATTENDANCE
    // ===========================

    @Transactional
    public void markAttendance(UUID schoolId, UUID markedBy, MarkAttendanceRequest request) {
        if (request.getDate() == null) {
            throw new BadRequestException("Date is required");
        }
        if (request.getClassId() == null) {
            throw new BadRequestException("Class ID is required");
        }
        if (request.getRecords() == null || request.getRecords().isEmpty()) {
            throw new BadRequestException("Attendance records are required");
        }

        // Prevent future dates
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Cannot mark attendance for future dates");
        }

        // Prevent marking attendance on holidays
        if (holidayRepository.existsBySchoolIdAndDate(schoolId, request.getDate())) {
            throw new BadRequestException("Cannot mark attendance on a holiday: " + request.getDate());
        }

        // Validate status values
        for (MarkAttendanceRequest.StudentAttendanceRecord record : request.getRecords()) {
            validateStatus(record.getStatus());
        }

        // For each record, upsert attendance
        for (MarkAttendanceRequest.StudentAttendanceRecord record : request.getRecords()) {
            Optional<Attendance> existing = attendanceRepository.findByStudentIdAndDate(record.getStudentId(), request.getDate());

            if (existing.isPresent()) {
                Attendance a = existing.get();
                a.setStatus(record.getStatus().toUpperCase());
                a.setRemarks(record.getRemarks());
                a.setClassId(request.getClassId());
                a.setMarkedBy(markedBy);
                attendanceRepository.save(a);
            } else {
                Attendance a = Attendance.builder()
                        .schoolId(schoolId)
                        .studentId(record.getStudentId())
                        .classId(request.getClassId())
                        .date(request.getDate())
                        .status(record.getStatus().toUpperCase())
                        .remarks(record.getRemarks())
                        .markedBy(markedBy)
                        .build();
                attendanceRepository.save(a);
            }
        }

        log.info("Attendance marked for class {} on {} - {} records", request.getClassId(), request.getDate(), request.getRecords().size());
    }

    @Transactional
    public void editAttendance(UUID schoolId, UUID attendanceId, String status, String remarks, UUID updatedBy) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", attendanceId));

        if (!attendance.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Attendance", "id", attendanceId);
        }

        validateStatus(status);
        attendance.setStatus(status.toUpperCase());
        if (remarks != null) {
            attendance.setRemarks(remarks);
        }
        attendance.setMarkedBy(updatedBy);
        attendanceRepository.save(attendance);

        log.info("Attendance {} updated to status {} by user {}", attendanceId, status, updatedBy);
    }

    // ===========================
    // GET ATTENDANCE
    // ===========================

    @Transactional(readOnly = true)
    public List<ClassAttendanceDto> getClassAttendanceForDate(UUID schoolId, UUID classId, LocalDate date) {
        List<Student> students = studentRepository.findActiveBySchoolIdAndClassId(schoolId, classId);
        List<Attendance> attendances = attendanceRepository.findBySchoolIdAndClassIdAndDate(schoolId, classId, date);

        Map<UUID, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(Attendance::getStudentId, a -> a, (a1, a2) -> a1));

        return students.stream().map(student -> {
            Attendance a = attendanceMap.get(student.getId());
            return ClassAttendanceDto.builder()
                    .id(a != null ? a.getId() : null)
                    .studentId(student.getId())
                    .studentName(student.getFullName())
                    .admissionNumber(student.getAdmissionNumber())
                    .classId(classId)
                    .className(null) // Frontend can resolve
                    .date(date)
                    .status(a != null ? a.getStatus() : null)
                    .remarks(a != null ? a.getRemarks() : null)
                    .markedBy(a != null ? a.getMarkedBy() : null)
                    .markedByName(a != null && a.getMarkedBy() != null
                            ? userRepository.findById(a.getMarkedBy()).map(User::getFullName).orElse(null)
                            : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getStudentAttendance(UUID schoolId, UUID studentId) {
        List<Attendance> records = attendanceRepository.findBySchoolIdAndStudentId(schoolId, studentId);

        return records.stream().map(a -> AttendanceResponse.builder()
                .id(a.getId())
                .studentId(a.getStudentId())
                .studentName(a.getStudent() != null ? a.getStudent().getFullName() : null)
                .classId(a.getClassId())
                .className(null)
                .date(a.getDate())
                .status(a.getStatus())
                .remarks(a.getRemarks())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getStudentAttendanceHistory(UUID studentId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> records;
        if (startDate != null && endDate != null) {
            records = attendanceRepository.findByStudentIdAndDateBetween(studentId, startDate, endDate);
        } else {
            records = attendanceRepository.findByStudentIdOrderByDateDesc(studentId);
        }

        return records.stream().map(a -> AttendanceResponse.builder()
                .id(a.getId())
                .studentId(a.getStudentId())
                .studentName(a.getStudent() != null ? a.getStudent().getFullName() : null)
                .classId(a.getClassId())
                .date(a.getDate())
                .status(a.getStatus())
                .remarks(a.getRemarks())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getChildrenAttendance(UUID parentId) {
        // Get parent's linked students
        List<ParentStudent> links = parentStudentRepository.findByParentId(parentId);
        List<UUID> studentIds = links.stream().map(ParentStudent::getStudentId).collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return List.of();
        }

        List<Attendance> records = attendanceRepository.findByStudentIdIn(studentIds);
        return records.stream().map(a -> AttendanceResponse.builder()
                .id(a.getId())
                .studentId(a.getStudentId())
                .studentName(a.getStudent() != null ? a.getStudent().getFullName() : null)
                .classId(a.getClassId())
                .date(a.getDate())
                .status(a.getStatus())
                .remarks(a.getRemarks())
                .build()
        ).collect(Collectors.toList());
    }

    // ===========================
    // REPORTS
    // ===========================

    @Transactional(readOnly = true)
    public AttendanceReportDto getClassAttendanceReport(UUID schoolId, UUID classId, LocalDate startDate, LocalDate endDate) {
        List<Student> students = studentRepository.findActiveBySchoolIdAndClassId(schoolId, classId);
        List<Attendance> attendances = attendanceRepository.findBySchoolIdAndClassIdAndDateBetween(schoolId, classId, startDate, endDate);

        // Exclude holidays from school day counts
        Set<LocalDate> holidaysInRange = holidayRepository.findBySchoolIdAndDateBetween(schoolId, startDate, endDate)
                .stream().map(com.schoolsaas.model.Holiday::getDate).collect(Collectors.toSet());

        // Calculate unique school days in this range, excluding holidays
        long totalSchoolDays = attendances.stream()
                .map(Attendance::getDate)
                .distinct()
                .filter(date -> !holidaysInRange.contains(date))
                .count();
        if (totalSchoolDays == 0) totalSchoolDays = 1; // avoid div/0

        // Count totals
        long presentCount = attendances.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long absentCount = attendances.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        long lateCount = attendances.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        long excusedCount = attendances.stream().filter(a -> "EXCUSED".equals(a.getStatus())).count();

        // Student-level reports
        Map<UUID, List<Attendance>> byStudent = attendances.stream().collect(Collectors.groupingBy(Attendance::getStudentId));
        List<AttendanceReportDto.StudentAttendanceReport> studentReports = students.stream().map(student -> {
            List<Attendance> studentAttendance = byStudent.getOrDefault(student.getId(), List.of());
            long present = studentAttendance.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
            long absent = studentAttendance.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
            long late = studentAttendance.stream().filter(a -> "LATE".equals(a.getStatus())).count();
            long excused = studentAttendance.stream().filter(a -> "EXCUSED".equals(a.getStatus())).count();
            long total = studentAttendance.size();
            double percentage = total > 0 ? ((present + late) * 100.0) / total : 0;

            return AttendanceReportDto.StudentAttendanceReport.builder()
                    .studentId(student.getId().toString())
                    .studentName(student.getFullName())
                    .admissionNumber(student.getAdmissionNumber())
                    .presentDays(present)
                    .absentDays(absent)
                    .lateDays(late)
                    .excusedDays(excused)
                    .totalDays(total)
                    .attendancePercentage(BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP))
                    .build();
        }).collect(Collectors.toList());

        // Daily summaries
        Map<LocalDate, List<Attendance>> byDate = attendances.stream().collect(Collectors.groupingBy(Attendance::getDate));
        List<AttendanceReportDto.DailyAttendanceSummary> dailySummaries = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<Attendance> dayAttendance = entry.getValue();
                    return AttendanceReportDto.DailyAttendanceSummary.builder()
                            .date(entry.getKey().toString())
                            .presentCount(dayAttendance.stream().filter(a -> "PRESENT".equals(a.getStatus())).count())
                            .absentCount(dayAttendance.stream().filter(a -> "ABSENT".equals(a.getStatus())).count())
                            .lateCount(dayAttendance.stream().filter(a -> "LATE".equals(a.getStatus())).count())
                            .excusedCount(dayAttendance.stream().filter(a -> "EXCUSED".equals(a.getStatus())).count())
                            .totalCount(dayAttendance.size())
                            .build();
                }).collect(Collectors.toList());

        double avgPercentage = studentReports.isEmpty() ? 0 :
                studentReports.stream().mapToDouble(r -> r.getAttendancePercentage().doubleValue()).average().orElse(0);

        return AttendanceReportDto.builder()
                .totalSchoolDays(totalSchoolDays)
                .totalStudents(students.size())
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .excusedCount(excusedCount)
                .averageAttendancePercentage(BigDecimal.valueOf(avgPercentage).setScale(2, RoundingMode.HALF_UP))
                .studentReports(studentReports)
                .dailySummaries(dailySummaries)
                .build();
    }

    @Transactional(readOnly = true)
    public AttendanceSummary getStudentAttendanceSummary(UUID schoolId, UUID studentId) {
        List<Attendance> records = attendanceRepository.findBySchoolIdAndStudentId(schoolId, studentId);

        // Exclude holiday records
        List<LocalDate> dates = records.stream().map(Attendance::getDate).distinct().collect(Collectors.toList());
        if (!dates.isEmpty()) {
            LocalDate minDate = dates.stream().min(Comparator.naturalOrder()).orElse(null);
            LocalDate maxDate = dates.stream().max(Comparator.naturalOrder()).orElse(null);
            if (minDate != null && maxDate != null) {
                Set<LocalDate> holidays = holidayRepository.findBySchoolIdAndDateBetween(schoolId, minDate, maxDate)
                        .stream().map(com.schoolsaas.model.Holiday::getDate).collect(Collectors.toSet());
                records = records.stream().filter(r -> !holidays.contains(r.getDate())).collect(Collectors.toList());
            }
        }

        long total = records.size();
        long present = records.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long absent = records.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        long late = records.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        long excused = records.stream().filter(a -> "EXCUSED".equals(a.getStatus())).count();

        double percentage = total > 0 ? ((present + late) * 100.0) / total : 0;

        return AttendanceSummary.builder()
                .totalDays(total)
                .presentDays(present)
                .absentDays(absent)
                .lateDays(late)
                .excusedDays(excused)
                .attendancePercentage(Math.round(percentage * 100.0) / 100.0)
                .build();
    }

    // ===========================
    // BULK UPLOAD
    // ===========================

    @Transactional
    public int processBulkAttendance(UUID schoolId, UUID markedBy, List<BulkAttendanceRow> rows) {
        int successCount = 0;
        for (BulkAttendanceRow row : rows) {
            try {
                if (row.getDate() == null || row.getDate().isAfter(LocalDate.now())) {
                    continue; // Skip future dates or null dates
                }
                if (row.getStudentEmail() == null && row.getAdmissionNumber() == null && row.getStudentId() == null) {
                    continue;
                }

                // Skip holidays
                if (holidayRepository.existsBySchoolIdAndDate(schoolId, row.getDate())) {
                    continue;
                }

                Student student = resolveStudent(schoolId, row.getStudentId(), row.getStudentEmail(), row.getAdmissionNumber());
                if (student == null) continue;

                String status = normalizeStatus(row.getStatus());
                if (!isValidStatus(status)) continue;

                Optional<Attendance> existing = attendanceRepository.findByStudentIdAndDate(student.getId(), row.getDate());
                if (existing.isPresent()) {
                    Attendance a = existing.get();
                    a.setStatus(status);
                    a.setRemarks(row.getRemarks());
                    a.setClassId(student.getClassId());
                    a.setMarkedBy(markedBy);
                    attendanceRepository.save(a);
                } else {
                    Attendance a = Attendance.builder()
                            .schoolId(schoolId)
                            .studentId(student.getId())
                            .classId(student.getClassId())
                            .date(row.getDate())
                            .status(status)
                            .remarks(row.getRemarks())
                            .markedBy(markedBy)
                            .build();
                    attendanceRepository.save(a);
                }
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to process attendance row: {}", e.getMessage());
            }
        }
        return successCount;
    }

    // ===========================
    // HELPERS
    // ===========================

    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!normalized.equals("PRESENT") && !normalized.equals("ABSENT") && !normalized.equals("LATE") && !normalized.equals("EXCUSED")) {
            throw new BadRequestException("Invalid status: " + status + ". Must be PRESENT, ABSENT, LATE, or EXCUSED");
        }
    }

    private boolean isValidStatus(String status) {
        return status != null && (status.equals("PRESENT") || status.equals("ABSENT") || status.equals("LATE") || status.equals("EXCUSED"));
    }

    private String normalizeStatus(String status) {
        if (status == null) return null;
        String s = status.trim().toUpperCase();
        if (s.equals("P") || s.equals("YES") || s.equals("1")) return "PRESENT";
        if (s.equals("A") || s.equals("NO") || s.equals("0")) return "ABSENT";
        if (s.equals("L")) return "LATE";
        if (s.equals("E")) return "EXCUSED";
        if (s.equals("PRESENT") || s.equals("ABSENT") || s.equals("LATE") || s.equals("EXCUSED")) return s;
        return null;
    }

    private Student resolveStudent(UUID schoolId, UUID studentId, String email, String admissionNumber) {
        if (studentId != null) {
            return studentRepository.findById(studentId)
                    .filter(s -> s.getSchoolId().equals(schoolId))
                    .orElse(null);
        }
        if (admissionNumber != null && !admissionNumber.isBlank()) {
            return studentRepository.findBySchoolIdAndAdmissionNumber(schoolId, admissionNumber).orElse(null);
        }
        if (email != null && !email.isBlank()) {
            return studentRepository.findBySchoolIdAndEmail(schoolId, email).orElse(null);
        }
        return null;
    }

    // ===========================
    // TEMPLATE
    // ===========================

    public String generateAttendanceTemplate() {
        return "date,student_email,admission_number,status,remarks\n" +
               "2026-06-15,[EMAIL_REDACTED],[ADMISSION NUMBER_REDACTED],PRESENT,\n" +
               "2026-06-15,[EMAIL_REDACTED],[ADMISSION NUMBER_REDACTED],ABSENT,\n" +
               "2026-06-16,[EMAIL_REDACTED],[ADMISSION NUMBER_REDACTED],LATE,Came late due to traffic\n" +
               "2026-06-16,[EMAIL_REDACTED],[ADMISSION NUMBER_REDACTED],EXCUSED,Sick leave\n";
    }

}
