package com.schoolsaas.controller;

import com.schoolsaas.dto.attendance.AttendanceResponse;
import com.schoolsaas.dto.attendance.AttendanceSummary;
import com.schoolsaas.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/students/{studentId}/attendance")
    @PreAuthorize("hasPermission(#schoolId, 'student.attendance.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<List<AttendanceResponse>> getStudentAttendance(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(attendanceService.getStudentAttendance(schoolId, studentId));
    }

    @GetMapping("/students/{studentId}/attendance/summary")
    @PreAuthorize("hasPermission(#schoolId, 'student.attendance.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<AttendanceSummary> getStudentAttendanceSummary(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(attendanceService.getStudentAttendanceSummary(studentId));
    }
}
