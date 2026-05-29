package com.schoolsaas.controller;

import com.schoolsaas.dto.announcement.AnnouncementDto;
import com.schoolsaas.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    public ResponseEntity<AnnouncementDto> createAnnouncement(@PathVariable UUID schoolId, @RequestBody AnnouncementDto dto) {
        return ResponseEntity.ok(announcementService.createAnnouncement(schoolId, dto));
    }

    @GetMapping
    public ResponseEntity<Page<AnnouncementDto>> listAnnouncements(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(announcementService.listAnnouncements(schoolId, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AnnouncementDto>> getActiveAnnouncements(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(announcementService.getActiveAnnouncements(schoolId));
    }
}
