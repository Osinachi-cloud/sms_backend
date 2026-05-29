package com.schoolsaas.service;

import com.schoolsaas.dto.announcement.AnnouncementDto;
import com.schoolsaas.model.Announcement;
import com.schoolsaas.model.Notification;
import com.schoolsaas.repository.AnnouncementRepository;
import com.schoolsaas.repository.NotificationRepository;
import com.schoolsaas.repository.UserSchoolRepository;
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
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final NotificationRepository notificationRepository;
    private final UserSchoolRepository userSchoolRepository;

    @Transactional
    public AnnouncementDto createAnnouncement(UUID schoolId, AnnouncementDto dto) {
        Announcement announcement = Announcement.builder()
                .schoolId(schoolId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .targetAudience(dto.getTargetAudience())
                .priority(dto.getPriority())
                .isPinned(dto.getIsPinned())
                .expiresAt(dto.getExpiresAt())
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();
        announcement = announcementRepository.save(announcement);

        // Create notifications for target audience
        createNotificationsForAudience(announcement);

        return mapToDto(announcement);
    }

    private void createNotificationsForAudience(Announcement announcement) {
        UUID schoolId = announcement.getSchoolId();
        String target = announcement.getTargetAudience();

        List<UUID> targetUserIds = userSchoolRepository.findBySchoolId(schoolId).stream()
                .filter(us -> {
                    if ("ALL".equals(target)) return true;
                    // Simple role-based filtering - would need more robust role resolution in production
                    return true;
                })
                .map(us -> us.getUserId())
                .distinct()
                .collect(Collectors.toList());

        for (UUID userId : targetUserIds) {
            Notification notif = Notification.builder()
                    .userId(userId)
                    .schoolId(schoolId)
                    .title(announcement.getTitle())
                    .message(announcement.getContent().length() > 200 ? announcement.getContent().substring(0, 200) + "..." : announcement.getContent())
                    .type("ANNOUNCEMENT")
                    .entityType("ANNOUNCEMENT")
                    .entityId(announcement.getId())
                    .build();
            notificationRepository.save(notif);
        }
    }

    public Page<AnnouncementDto> listAnnouncements(UUID schoolId, Pageable pageable) {
        return announcementRepository.findBySchoolIdOrderByIsPinnedDescCreatedAtDesc(schoolId, pageable)
                .map(this::mapToDto);
    }

    public List<AnnouncementDto> getActiveAnnouncements(UUID schoolId) {
        return announcementRepository.findBySchoolIdAndExpiresAtAfterOrExpiresAtIsNullOrderByIsPinnedDescCreatedAtDesc(schoolId, LocalDateTime.now())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private AnnouncementDto mapToDto(Announcement a) {
        AnnouncementDto dto = new AnnouncementDto();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setContent(a.getContent());
        dto.setTargetAudience(a.getTargetAudience());
        dto.setPriority(a.getPriority());
        dto.setIsPinned(a.getIsPinned());
        dto.setExpiresAt(a.getExpiresAt());
        dto.setCreatedBy(a.getCreatedBy());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }
}
