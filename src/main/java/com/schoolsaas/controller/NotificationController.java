package com.schoolsaas.controller;

import com.schoolsaas.dto.notification.NotificationDto;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> listNotifications(Pageable pageable) {
        return ResponseEntity.ok(notificationService.listNotifications(SecurityUtils.getCurrentUserId(), pageable));
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationDto>> getUnreadNotifications(Pageable pageable) {
        List<NotificationDto> list = notificationService.getUnreadNotifications(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}
