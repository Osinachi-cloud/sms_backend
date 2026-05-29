package com.schoolsaas.controller;

import com.schoolsaas.model.TeacherActivityLog;
import com.schoolsaas.service.TeacherActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/teacher-activities")
@RequiredArgsConstructor
public class TeacherActivityController {

    private final TeacherActivityLogService activityLogService;

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<Page<TeacherActivityLog>> getTeacherActivities(
            @PathVariable UUID schoolId,
            @PathVariable UUID teacherId,
            Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getTeacherActivities(schoolId, teacherId, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<TeacherActivityLog>> getSchoolActivities(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getSchoolActivities(schoolId, pageable));
    }
}
