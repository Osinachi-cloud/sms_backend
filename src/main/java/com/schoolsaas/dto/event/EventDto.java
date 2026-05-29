package com.schoolsaas.dto.event;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class EventDto {
    private UUID id;
    private String title;
    private String description;
    private String eventType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private Boolean isAllDay;
    private Boolean isRecurring;
    private String recurrenceRule;
    private UUID createdBy;
    private List<UUID> attendeeIds;
}
