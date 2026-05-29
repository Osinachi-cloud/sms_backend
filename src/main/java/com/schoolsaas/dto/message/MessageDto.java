package com.schoolsaas.dto.message;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MessageDto {
    private UUID id;
    private UUID senderId;
    private String senderName;
    private String content;
    private String messageType;
    private String fileUrl;
    private LocalDateTime createdAt;
}
