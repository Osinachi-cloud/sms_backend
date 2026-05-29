package com.schoolsaas.controller;

import com.schoolsaas.dto.ai.ChatRequest;
import com.schoolsaas.dto.ai.ChatResponse;
import com.schoolsaas.security.UserPrincipal;
import com.schoolsaas.service.AiTutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-tutor")
@RequiredArgsConstructor
public class AiTutorController {

    private final AiTutorService aiTutorService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = aiTutorService.chat(userPrincipal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam(defaultValue = "dashboard") String page) {
        return ResponseEntity.ok(aiTutorService.getSuggestedQuestions(page));
    }
}
