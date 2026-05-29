package com.schoolsaas.dto.notification;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationDto {
    private UUID id;
    private String title;
    private String message;
    private String type;
    private String entityType;
    private UUID entityId;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
