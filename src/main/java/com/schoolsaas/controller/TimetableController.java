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

    @PutMapping("/periods/{periodId}")
    @PreAuthorize("hasPermission(#schoolId, 'timetable.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<TimetablePeriodDto> updatePeriod(@PathVariable UUID schoolId, @PathVariable UUID periodId, @RequestBody TimetablePeriodDto dto) {
        return ResponseEntity.ok(timetableService.updatePeriod(schoolId, periodId, dto));
    }

    @DeleteMapping("/periods/{periodId}")
    @PreAuthorize("hasPermission(#schoolId, 'timetable.delete') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deletePeriod(@PathVariable UUID schoolId, @PathVariable UUID periodId) {
        timetableService.deletePeriod(schoolId, periodId);
        return ResponseEntity.ok().build();
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

    @PutMapping("/entries/{entryId}")
    @PreAuthorize("hasPermission(#schoolId, 'timetable.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<TimetableEntryDto> updateEntry(@PathVariable UUID schoolId, @PathVariable UUID entryId, @RequestBody TimetableEntryDto dto) {
        return ResponseEntity.ok(timetableService.updateEntry(schoolId, entryId, dto));
    }

    @DeleteMapping("/entries/{entryId}")
    @PreAuthorize("hasPermission(#schoolId, 'timetable.delete') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteEntry(@PathVariable UUID schoolId, @PathVariable UUID entryId) {
        timetableService.deleteEntry(schoolId, entryId);
        return ResponseEntity.ok().build();
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
