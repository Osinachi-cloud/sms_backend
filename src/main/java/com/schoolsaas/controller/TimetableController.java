package com.schoolsaas.controller;

import com.schoolsaas.dto.timetable.TimetableEntryDto;
import com.schoolsaas.dto.timetable.TimetablePeriodDto;
import com.schoolsaas.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @PostMapping("/periods")
    @PreAuthorize("hasPermission(#schoolId, 'timetable.create') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<TimetablePeriodDto> createPeriod(@PathVariable UUID schoolId, @RequestBody TimetablePeriodDto dto) {
        return ResponseEntity.ok(timetableService.createPeriod(schoolId, dto));
    }

    @GetMapping("/periods")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TimetablePeriodDto>> getPeriods(@PathVariable UUID schoolId, Pageable pageable) {
        List<TimetablePeriodDto> list = timetableService.getPeriods(schoolId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @PostMapping("/entries")
    @PreAuthorize("hasPermission(#schoolId, 'timetable.create') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<TimetableEntryDto> createEntry(@PathVariable UUID schoolId, @RequestBody TimetableEntryDto dto) {
        return ResponseEntity.ok(timetableService.createEntry(schoolId, dto));
    }

    @GetMapping("/classes/{classId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TimetableEntryDto>> getClassTimetable(@PathVariable UUID schoolId, @PathVariable UUID classId) {
        return ResponseEntity.ok(timetableService.getClassTimetable(schoolId, classId));
    }

    @GetMapping("/teachers/{teacherId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TimetableEntryDto>> getTeacherTimetable(@PathVariable UUID schoolId, @PathVariable UUID teacherId) {
        return ResponseEntity.ok(timetableService.getTeacherTimetable(schoolId, teacherId));
    }
}
