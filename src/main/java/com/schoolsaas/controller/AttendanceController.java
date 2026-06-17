package com.schoolsaas.controller;

import com.schoolsaas.dto.attendance.*;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.AttendanceService;
import com.schoolsaas.service.BulkEnrollmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final BulkEnrollmentService bulkEnrollmentService;

    // ===========================
    // CLASS ATTENDANCE (Teacher)
    // ===========================

    @GetMapping("/class/{classId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassAttendanceDto>> getClassAttendanceForDate(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(attendanceService.getClassAttendanceForDate(schoolId, classId, date));
    }

    @GetMapping("/class/{classId}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceReportDto> getClassAttendanceReport(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(attendanceService.getClassAttendanceReport(schoolId, classId, startDate, endDate));
    }

    @PostMapping("/mark")
    @PreAuthorize("hasPermission(#schoolId, 'student.attendance.manage') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Map<String, String>> markAttendance(
            @PathVariable UUID schoolId,
            @RequestBody MarkAttendanceRequest request) {
        UUID markedBy = SecurityUtils.getCurrentUserId();
        attendanceService.markAttendance(schoolId, markedBy, request);
        return ResponseEntity.ok(Map.of("message", "Attendance saved successfully"));
    }

    @PutMapping("/{attendanceId}")
    @PreAuthorize("hasPermission(#schoolId, 'student.attendance.manage') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Map<String, String>> editAttendance(
            @PathVariable UUID schoolId,
            @PathVariable UUID attendanceId,
            @RequestBody Map<String, String> body) {
        UUID updatedBy = SecurityUtils.getCurrentUserId();
        String status = body.get("status");
        String remarks = body.get("remarks");
        attendanceService.editAttendance(schoolId, attendanceId, status, remarks, updatedBy);
        return ResponseEntity.ok(Map.of("message", "Attendance updated successfully"));
    }

    // ===========================
    // STUDENT ATTENDANCE
    // ===========================

    @GetMapping("/students/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttendanceResponse>> getStudentAttendance(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(attendanceService.getStudentAttendanceHistory(studentId, startDate, endDate));
    }

    @GetMapping("/students/{studentId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceSummary> getStudentAttendanceSummary(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(attendanceService.getStudentAttendanceSummary(schoolId, studentId));
    }

    // ===========================
    // PARENT ATTENDANCE
    // ===========================

    @GetMapping("/parents/{parentId}/children")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttendanceResponse>> getChildrenAttendance(
            @PathVariable UUID schoolId,
            @PathVariable UUID parentId) {
        return ResponseEntity.ok(attendanceService.getChildrenAttendance(parentId));
    }

    // ===========================
    // BULK UPLOAD
    // ===========================

    @PostMapping("/bulk-upload")
    @PreAuthorize("hasPermission(#schoolId, 'student.attendance.manage') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Map<String, Object>> bulkUploadAttendance(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file) throws Exception {
        UUID markedBy = SecurityUtils.getCurrentUserId();

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BadRequestException("File name is required");
        }

        List<BulkAttendanceRow> rows;
        if (filename.endsWith(".csv")) {
            rows = BulkAttendanceCsvParser.parse(file.getInputStream());
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            rows = BulkAttendanceExcelParser.parse(file.getInputStream());
        } else {
            throw new BadRequestException("Unsupported file format. Use CSV or Excel (.xlsx)");
        }

        int successCount = attendanceService.processBulkAttendance(schoolId, markedBy, rows);

        return ResponseEntity.ok(Map.of(
                "message", "Bulk attendance processed",
                "totalRows", rows.size(),
                "successfulRows", successCount
        ));
    }

    @GetMapping("/template")
    @PreAuthorize("hasPermission(#schoolId, 'student.attendance.manage') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public void downloadAttendanceTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_template.csv");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = response.getWriter()) {
            writer.write(attendanceService.generateAttendanceTemplate());
        }
    }
}
