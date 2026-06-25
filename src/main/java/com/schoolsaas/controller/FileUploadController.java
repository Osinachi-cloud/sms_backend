package com.schoolsaas.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/schools/{schoolId}/uploads")
@RequiredArgsConstructor
public class FileUploadController {

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadFile(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "general") String category) throws IOException {
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        // Sanitize filename
        String safeName = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = timestamp + "_" + safeName;

        // Store in uploads directory
        Path uploadPath = Paths.get("uploads", schoolId.toString(), category);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        // Return relative URL
        String fileUrl = "/uploads/" + schoolId + "/" + category + "/" + fileName;
        log.info("File uploaded: {} for school: {} category: {}", fileUrl, schoolId, category);

        return ResponseEntity.ok(Map.of(
            "url", fileUrl,
            "name", originalFilename,
            "fullUrl", fileUrl
        ));
    }
}
