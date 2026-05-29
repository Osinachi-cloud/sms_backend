package com.schoolsaas.dto.message;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ConversationDto {
    private UUID id;
    private String title;
    private String type;
    private List<ParticipantDto> participants;
    private MessageDto lastMessage;
    private long unreadCount;
    private LocalDateTime updatedAt;
}
