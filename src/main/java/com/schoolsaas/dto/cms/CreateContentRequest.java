package com.schoolsaas.dto.cms;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class CreateContentRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String contentType = "NOTE";
    private UUID folderId;
    private UUID subjectId;
    private UUID termId;
    private UUID sessionId;
    private List<UUID> targetClassIds;
    private String richText;
    private List<String> fileUrls;
    private List<String> videoLinks;
    private LocalDateTime dueDate;
    private LocalDateTime expiresAt;
    private Map<String, Object> metadata;
    private String body;
    private List<String> tags;
    private String thumbnailUrl;
    private List<String> targetAudience;
}
