package com.schoolsaas.dto.announcement;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AnnouncementDto {
    private UUID id;
    private String title;
    private String content;
    private String targetAudience;
    private String priority;
    private Boolean isPinned;
    private LocalDateTime expiresAt;
    private UUID createdBy;
    private LocalDateTime createdAt;
}
