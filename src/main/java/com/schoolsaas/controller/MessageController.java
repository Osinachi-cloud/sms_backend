package com.schoolsaas.controller;

import com.schoolsaas.dto.message.ConversationDto;
import com.schoolsaas.dto.message.MessageDto;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessagingService messagingService;

    @PostMapping("/conversations")
    public ResponseEntity<ConversationDto> createConversation(@PathVariable UUID schoolId, @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        @SuppressWarnings("unchecked")
        List<String> participantIds = (List<String>) body.get("participantIds");
        List<UUID> ids = participantIds.stream().map(UUID::fromString).toList();
        return ResponseEntity.ok(messagingService.createConversation(schoolId, title, ids));
    }

    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationDto>> listConversations(@PathVariable UUID schoolId, Pageable pageable) {
        List<ConversationDto> list = messagingService.listConversations(schoolId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageDto> sendMessage(@PathVariable UUID conversationId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(messagingService.sendMessage(conversationId, body.get("content"), body.getOrDefault("messageType", "TEXT"), body.get("fileUrl")));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<MessageDto>> getMessages(@PathVariable UUID conversationId, Pageable pageable) {
        return ResponseEntity.ok(messagingService.getMessages(conversationId, pageable));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID schoolId, @PathVariable UUID conversationId) {
        messagingService.markConversationAsRead(conversationId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}
