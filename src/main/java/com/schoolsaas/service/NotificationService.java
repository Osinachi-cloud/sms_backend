package com.schoolsaas.service;

import com.schoolsaas.dto.notification.NotificationDto;
import com.schoolsaas.model.Notification;
import com.schoolsaas.repository.NotificationRepository;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.repository.StudentSubjectEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public void sendNotification(UUID userId, UUID schoolId, String title, String message, String type, UUID entityId) {
        Notification notif = Notification.builder()
                .userId(userId)
                .schoolId(schoolId)
                .title(title)
                .message(message)
                .type(type)
                .entityId(entityId)
                .build();
        notificationRepository.save(notif);
    }

    public Page<NotificationDto> listNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::mapToDto);
    }

    public List<NotificationDto> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            n.setReadAt(java.time.LocalDateTime.now());
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void notifySubjectContentUploaded(UUID schoolId, UUID subjectId, String contentTitle, String subjectName) {
        List<com.schoolsaas.model.StudentSubjectEnrollment> enrollments = enrollmentRepository.findBySchoolIdAndSubjectId(schoolId, subjectId);
        for (com.schoolsaas.model.StudentSubjectEnrollment enrollment : enrollments) {
            if (!"ENROLLED".equals(enrollment.getStatus())) continue;
            studentRepository.findById(enrollment.getStudentId()).ifPresent(student -> {
                if (student.getUserId() != null) {
                    sendNotification(
                            student.getUserId(),
                            schoolId,
                            "New Course Material: " + subjectName,
                            "A new material \"" + contentTitle + "\" has been uploaded for " + subjectName + ".",
                            "COURSE_CONTENT",
                            subjectId
                    );
                }
            });
        }
    }

    private NotificationDto mapToDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setType(n.getType());
        dto.setEntityType(n.getEntityType());
        dto.setEntityId(n.getEntityId());
        dto.setIsRead(n.getIsRead());
        dto.setReadAt(n.getReadAt());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
