package com.schoolsaas.controller;

import com.schoolsaas.dto.timetable.TimetableEntryDto;
import com.schoolsaas.dto.timetable.TimetablePeriodDto;
import com.schoolsaas.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @PostMapping("/periods")
    public ResponseEntity<TimetablePeriodDto> createPeriod(@PathVariable UUID schoolId, @RequestBody TimetablePeriodDto dto) {
        return ResponseEntity.ok(timetableService.createPeriod(schoolId, dto));
    }

    @GetMapping("/periods")
    public ResponseEntity<List<TimetablePeriodDto>> getPeriods(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(timetableService.getPeriods(schoolId));
    }

    @PostMapping("/entries")
    public ResponseEntity<TimetableEntryDto> createEntry(@PathVariable UUID schoolId, @RequestBody TimetableEntryDto dto) {
        return ResponseEntity.ok(timetableService.createEntry(schoolId, dto));
    }

    @GetMapping("/classes/{classId}")
    public ResponseEntity<List<TimetableEntryDto>> getClassTimetable(@PathVariable UUID schoolId, @PathVariable UUID classId) {
        return ResponseEntity.ok(timetableService.getClassTimetable(schoolId, classId));
    }

    @GetMapping("/teachers/{teacherId}")
    public ResponseEntity<List<TimetableEntryDto>> getTeacherTimetable(@PathVariable UUID schoolId, @PathVariable UUID teacherId) {
        return ResponseEntity.ok(timetableService.getTeacherTimetable(schoolId, teacherId));
    }
}
