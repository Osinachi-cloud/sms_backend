package com.schoolsaas.service;

import com.schoolsaas.dto.event.EventDto;
import com.schoolsaas.model.Event;
import com.schoolsaas.model.EventAttendee;
import com.schoolsaas.repository.EventAttendeeRepository;
import com.schoolsaas.repository.EventRepository;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final NotificationService notificationService;

    @Transactional
    public EventDto createEvent(UUID schoolId, EventDto dto) {
        Event event = Event.builder()
                .schoolId(schoolId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .eventType(dto.getEventType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .location(dto.getLocation())
                .isAllDay(dto.getIsAllDay())
                .isRecurring(dto.getIsRecurring())
                .recurrenceRule(dto.getRecurrenceRule())
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();
        event = eventRepository.save(event);

        if (dto.getAttendeeIds() != null) {
            for (UUID userId : dto.getAttendeeIds()) {
                attendeeRepository.save(EventAttendee.builder()
                        .eventId(event.getId())
                        .userId(userId)
                        .status("INVITED")
                        .build());
                notificationService.sendNotification(userId, schoolId, "Event Invitation",
                        "You are invited to: " + dto.getTitle(), "EVENT", event.getId());
            }
        }

        return mapToDto(event);
    }

    public Page<EventDto> listEvents(UUID schoolId, Pageable pageable) {
        return eventRepository.findBySchoolId(schoolId, pageable).map(this::mapToDto);
    }

    public List<EventDto> getUpcomingEvents(UUID schoolId) {
        return eventRepository.findBySchoolIdAndStartDateAfterOrderByStartDateAsc(schoolId, LocalDateTime.now())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public EventDto getEvent(UUID eventId) {
        return eventRepository.findById(eventId).map(this::mapToDto).orElseThrow();
    }

    private EventDto mapToDto(Event event) {
        EventDto dto = new EventDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setEventType(event.getEventType());
        dto.setStartDate(event.getStartDate());
        dto.setEndDate(event.getEndDate());
        dto.setLocation(event.getLocation());
        dto.setIsAllDay(event.getIsAllDay());
        dto.setIsRecurring(event.getIsRecurring());
        dto.setRecurrenceRule(event.getRecurrenceRule());
        dto.setCreatedBy(event.getCreatedBy());
        dto.setAttendeeIds(attendeeRepository.findByEventId(event.getId()).stream()
                .map(EventAttendee::getUserId).collect(Collectors.toList()));
        return dto;
    }
}
