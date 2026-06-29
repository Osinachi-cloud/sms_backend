package com.schoolsaas.controller;

import com.schoolsaas.service.EmailService;
import com.schoolsaas.service.EmailService.EmailAttachment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, String>> broadcastEmail(
            @PathVariable UUID schoolId,
            @RequestBody BroadcastEmailRequest request) {

        if (request.recipients == null || request.recipients.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No recipients provided"));
        }

        emailService.sendEmailWithAttachment(
                request.recipients,
                request.subject,
                request.htmlBody,
                null
        );

        return ResponseEntity.ok(Map.of(
                "message", "Emails queued for delivery",
                "count", String.valueOf(request.recipients.size())
        ));
    }

    public record BroadcastEmailRequest(
            List<String> recipients,
            String subject,
            String htmlBody
    ) {}
}
