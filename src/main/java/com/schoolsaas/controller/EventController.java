package com.schoolsaas.controller;

import com.schoolsaas.dto.event.EventDto;
import com.schoolsaas.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventDto> createEvent(@PathVariable UUID schoolId, @RequestBody EventDto dto) {
        return ResponseEntity.ok(eventService.createEvent(schoolId, dto));
    }

    @GetMapping
    public ResponseEntity<Page<EventDto>> listEvents(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(eventService.listEvents(schoolId, pageable));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<EventDto>> getUpcomingEvents(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(eventService.getUpcomingEvents(schoolId));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }
}
