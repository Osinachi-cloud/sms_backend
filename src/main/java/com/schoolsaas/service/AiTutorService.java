package com.schoolsaas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.dto.ai.ChatMessage;
import com.schoolsaas.dto.ai.ChatRequest;
import com.schoolsaas.dto.ai.ChatResponse;
import com.schoolsaas.model.User;
import com.schoolsaas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTutorService {

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_PROMPT = """
        You are an intelligent AI tutor assistant for a school management system. Your role is to help users
        (administrators, teachers, students, and parents) navigate and use the platform effectively.

        Key capabilities you can help with:
        - Explaining how to use different features of the school management system
        - Guiding users through student enrollment, attendance tracking, grade management
        - Helping teachers with content management and lesson planning
        - Assisting with fee payment processes and financial reports
        - Answering questions about the platform's role-based permissions
        - Providing step-by-step tutorials for common tasks

        Guidelines:
        - Be friendly, patient, and professional
        - Provide clear, step-by-step instructions when explaining processes
        - If you're unsure about something, say so and suggest contacting support
        - Keep responses concise but comprehensive
        - Use simple language that anyone can understand
        - When relevant, mention specific menu items or buttons in the interface

        The platform has these main modules:
        - Dashboard: Overview and quick actions
        - Students: Student management, bulk enrollment, profiles
        - Teachers: Teacher management, class assignments
        - Classes: Class/grade management
        - CMS: Content management with approval workflow
        - Payments: Fee structures, payment tracking (Paystack integration)
        - Reports: Academic and financial analytics
        - Settings: User preferences, roles & permissions
        """;

    public ChatResponse chat(UUID userId, ChatRequest request) {
        /* OpenAI implementation commented out for now
        User user = userRepository.findById(userId).orElse(null);
        String userContext = buildUserContext(user, request.getContext());

        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
            "role", "system",
            "content", SYSTEM_PROMPT + "\n\nCurrent user context:\n" + userContext
        ));

        if (request.getHistory() != null) {
            for (ChatMessage msg : request.getHistory()) {
                messages.add(Map.of(
                    "role", msg.getRole(),
                    "content", msg.getContent()
                ));
            }
        }

        messages.add(Map.of(
            "role", "user",
            "content", request.getMessage()
        ));

        try {
            String response = callOpenAI(messages);
            return ChatResponse.builder()
                    .message(response)
                    .timestamp(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.error("AI Tutor error for user {}: {}", userId, e.getMessage());
            return ChatResponse.builder()
                    .message("I apologize, but I'm having trouble processing your request right now. Please try again in a moment or contact support if the issue persists.")
                    .timestamp(System.currentTimeMillis())
                    .error(true)
                    .build();
        }
        */
        return ChatResponse.builder()
                .message("The AI Tutor feature is currently disabled. Please contact your administrator for more information.")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private String buildUserContext(User user, Map<String, String> context) {
        StringBuilder sb = new StringBuilder();

        if (user != null) {
            sb.append("User: ").append(user.getFullName()).append("\n");
            sb.append("Email: ").append(user.getEmail()).append("\n");
        }

        if (context != null) {
            if (context.containsKey("currentPage")) {
                sb.append("Current page: ").append(context.get("currentPage")).append("\n");
            }
            if (context.containsKey("schoolName")) {
                sb.append("School: ").append(context.get("schoolName")).append("\n");
            }
            if (context.containsKey("role")) {
                sb.append("Role: ").append(context.get("role")).append("\n");
            }
        }

        return sb.toString();
    }

    private String callOpenAI(List<Map<String, String>> messages) {
        /* OpenAI implementation commented out for now
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openaiModel);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            OPENAI_API_URL,
            HttpMethod.POST,
            entity,
            String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
        */
        return "AI implementation is disabled.";
    }

    public List<String> getSuggestedQuestions(String currentPage) {
        Map<String, List<String>> pageQuestions = new HashMap<>();

        pageQuestions.put("dashboard", List.of(
            "How do I view my school's performance metrics?",
            "What do the dashboard statistics mean?",
            "How can I quickly add a new student?"
        ));

        pageQuestions.put("students", List.of(
            "How do I enroll multiple students at once?",
            "How can I search for a specific student?",
            "What information is required for student enrollment?"
        ));

        pageQuestions.put("teachers", List.of(
            "How do I assign a teacher to a class?",
            "How can I view a teacher's schedule?",
            "What permissions can teachers have?"
        ));

        pageQuestions.put("cms", List.of(
            "How do I create new learning content?",
            "What is the content approval process?",
            "How do I organize content into folders?"
        ));

        pageQuestions.put("payments", List.of(
            "How do I record a payment?",
            "How does the Paystack integration work?",
            "How can I generate payment reports?"
        ));

        pageQuestions.put("settings", List.of(
            "How do I create a custom role?",
            "How can I change my password?",
            "What permissions are available?"
        ));

        return pageQuestions.getOrDefault(currentPage.toLowerCase(), List.of(
            "How can I navigate this platform?",
            "What features are available to me?",
            "How do I get support?"
        ));
    }
}
