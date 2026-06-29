package com.schoolsaas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public record EmailAttachment(String filename, byte[] content, String contentType) {}

    @Async
    public void sendEmailWithAttachment(String to, String subject, String htmlBody, EmailAttachment attachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (attachment != null) {
                helper.addAttachment(attachment.filename(), new ByteArrayResource(attachment.content()));
            }

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
        }
    }

    @Async
    public void sendEmailWithAttachment(List<String> toList, String subject, String htmlBody, EmailAttachment attachment) {
        for (String to : toList) {
            sendEmailWithAttachment(to, subject, htmlBody, attachment);
        }
    }
}
