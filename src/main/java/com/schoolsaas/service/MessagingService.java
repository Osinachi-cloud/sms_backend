package com.schoolsaas.service;

import com.schoolsaas.dto.message.ConversationDto;
import com.schoolsaas.dto.message.MessageDto;
import com.schoolsaas.dto.message.ParticipantDto;
import com.schoolsaas.model.Conversation;
import com.schoolsaas.model.ConversationParticipant;
import com.schoolsaas.model.Message;
import com.schoolsaas.model.User;
import com.schoolsaas.repository.*;
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
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ConversationDto createConversation(UUID schoolId, String title, List<UUID> participantIds) {
        Conversation conv = Conversation.builder()
                .schoolId(schoolId)
                .title(title)
                .type(participantIds.size() > 2 ? "GROUP" : "DIRECT")
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();
        conv = conversationRepository.save(conv);

        for (UUID userId : participantIds) {
            participantRepository.save(ConversationParticipant.builder()
                    .conversationId(conv.getId())
                    .userId(userId)
                    .build());
        }

        return mapToDto(conv);
    }

    @Transactional
    public MessageDto sendMessage(UUID conversationId, String content, String messageType, String fileUrl) {
        UUID senderId = SecurityUtils.getCurrentUserId();
        Message msg = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .messageType(messageType)
                .fileUrl(fileUrl)
                .build();
        msg = messageRepository.save(msg);

        // Notify other participants
        List<UUID> participants = participantRepository.findByConversationId(conversationId).stream()
                .map(ConversationParticipant::getUserId)
                .filter(uid -> !uid.equals(senderId))
                .collect(Collectors.toList());

        for (UUID userId : participants) {
            notificationService.sendNotification(userId, null, "New Message",
                    content.length() > 100 ? content.substring(0, 100) + "..." : content,
                    "MESSAGE", conversationId);
        }

        return mapToDto(msg);
    }

    public List<ConversationDto> listConversations(UUID schoolId, UUID userId) {
        List<Conversation> conversations = conversationRepository.findByParticipantUserIdAndSchoolId(userId, schoolId);
        return conversations.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public Page<MessageDto> getMessages(UUID conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public void markConversationAsRead(UUID conversationId, UUID userId) {
        participantRepository.findByConversationIdAndUserId(conversationId, userId).ifPresent(p -> {
            p.setLastReadAt(LocalDateTime.now());
            participantRepository.save(p);
        });
    }

    private ConversationDto mapToDto(Conversation conv) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conv.getId());
        dto.setTitle(conv.getTitle());
        dto.setType(conv.getType());
        dto.setUpdatedAt(conv.getUpdatedAt());

        List<ParticipantDto> participants = participantRepository.findByConversationId(conv.getId()).stream()
                .map(p -> {
                    ParticipantDto pd = new ParticipantDto();
                    User u = userRepository.findById(p.getUserId()).orElse(null);
                    if (u != null) {
                        pd.setUserId(u.getId());
                        pd.setFullName(u.getFullName());
                        pd.setAvatarUrl(u.getAvatarUrl());
                    }
                    return pd;
                }).collect(Collectors.toList());
        dto.setParticipants(participants);

        List<Message> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId());
        if (!msgs.isEmpty()) {
            dto.setLastMessage(mapToDto(msgs.get(msgs.size() - 1)));
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ConversationParticipant cp = participantRepository.findByConversationIdAndUserId(conv.getId(), currentUserId).orElse(null);
        if (cp != null && cp.getLastReadAt() != null) {
            dto.setUnreadCount(messageRepository.countByConversationIdAndCreatedAtAfter(conv.getId(), cp.getLastReadAt()));
        } else {
            dto.setUnreadCount(msgs.size());
        }

        return dto;
    }

    private MessageDto mapToDto(Message msg) {
        MessageDto dto = new MessageDto();
        dto.setId(msg.getId());
        dto.setSenderId(msg.getSenderId());
        User u = userRepository.findById(msg.getSenderId()).orElse(null);
        if (u != null) dto.setSenderName(u.getFullName());
        dto.setContent(msg.getContent());
        dto.setMessageType(msg.getMessageType());
        dto.setFileUrl(msg.getFileUrl());
        dto.setCreatedAt(msg.getCreatedAt());
        return dto;
    }
}
