package com.schoolsaas.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.schoolsaas.dto.payment.InitiatePaymentRequest;
import com.schoolsaas.dto.payment.PaymentResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.Payment;
import com.schoolsaas.model.PaymentGatewayType;
import com.schoolsaas.model.SchoolPaymentGatewayConfig;
import com.schoolsaas.model.Student;
import com.schoolsaas.repository.PaymentRepository;
import com.schoolsaas.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackProvider implements PaymentGatewayProvider {

    @Value("${paystack.secret-key}")
    private String globalSecretKey;

    @Value("${paystack.base-url}")
    private String baseUrl;

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getGatewayName() {
        return "PAYSTACK";
    }

    private String resolveSecretKey(SchoolPaymentGatewayConfig config) {
        if (config != null && config.getPaystackSecretKey() != null && !config.getPaystackSecretKey().isBlank()) {
            return config.getPaystackSecretKey();
        }
        return globalSecretKey;
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(UUID schoolId, InitiatePaymentRequest request, SchoolPaymentGatewayConfig config) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", request.getStudentId());
        }

        String reference = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        BigDecimal amountInKobo = request.getAmount().multiply(BigDecimal.valueOf(100));

        Payment payment = Payment.builder()
                .schoolId(schoolId)
                .studentId(request.getStudentId())
                .studentFeeId(request.getStudentFeeId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "NGN")
                .paymentReference(reference)
                .gateway(PaymentGatewayType.PAYSTACK)
                .status("PENDING")
                .build();

        String secretKey = resolveSecretKey(config);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(secretKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("email", student.getEmail() != null ? student.getEmail() : student.getParentEmail());
            body.put("amount", amountInKobo.intValue());
            body.put("reference", reference);
            body.put("currency", payment.getCurrency());
            if (request.getCallbackUrl() != null) {
                body.put("callback_url", request.getCallbackUrl());
            }

            Map<String, String> metadata = new HashMap<>();
            metadata.put("school_id", schoolId.toString());
            metadata.put("student_id", student.getId().toString());
            metadata.put("student_name", student.getFullName());
            if (request.getSubjectId() != null) {
                metadata.put("subject_id", request.getSubjectId().toString());
            }
            if (request.getDescription() != null) {
                metadata.put("description", request.getDescription());
            }
            body.put("metadata", metadata);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/transaction/initialize",
                    entity,
                    String.class
            );

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            if (jsonResponse.get("status").asBoolean()) {
                JsonNode data = jsonResponse.get("data");
                payment.setPaystackReference(data.get("reference").asText());
                payment.setPaystackAccessCode(data.get("access_code").asText());

                payment = paymentRepository.save(payment);

                PaymentResponse paymentResponse = PaymentResponse.fromEntity(payment);
                paymentResponse.setAuthorizationUrl(data.get("authorization_url").asText());
                return paymentResponse;
            } else {
                throw new BadRequestException("Payment initialization failed: " + jsonResponse.get("message").asText());
            }

        } catch (Exception e) {
            log.error("Error initializing Paystack payment", e);
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            throw new BadRequestException("Payment initialization failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(String reference, SchoolPaymentGatewayConfig config) {
        Payment payment = paymentRepository.findByPaymentReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "reference", reference));

        String secretKey = resolveSecretKey(config);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(secretKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/transaction/verify/" + reference,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            if (jsonResponse.get("status").asBoolean()) {
                JsonNode data = jsonResponse.get("data");
                String paystackStatus = data.get("status").asText();

                if ("success".equals(paystackStatus)) {
                    payment.setStatus("SUCCESS");
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setPaymentMethod(data.has("channel") ? data.get("channel").asText() : "card");
                } else if ("failed".equals(paystackStatus)) {
                    payment.setStatus("FAILED");
                } else {
                    payment.setStatus("ABANDONED");
                }

                Map<String, Object> metadata = new HashMap<>();
                if (data.has("metadata")) {
                    metadata = objectMapper.convertValue(data.get("metadata"), Map.class);
                }
                metadata.put("gateway_response", data.has("gateway_response") ? data.get("gateway_response").asText() : null);
                payment.setMetadata(metadata);

                payment = paymentRepository.save(payment);
                log.info("Payment verified: {} status: {}", reference, payment.getStatus());
            }

        } catch (Exception e) {
            log.error("Error verifying Paystack payment", e);
            throw new BadRequestException("Payment verification failed: " + e.getMessage());
        }

        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, SchoolPaymentGatewayConfig config) {
        log.info("Received Paystack webhook");
        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.get("event").asText();

            if ("charge.success".equals(eventType)) {
                JsonNode data = event.get("data");
                String reference = data.get("reference").asText();
                verifyPayment(reference, config);
            }
        } catch (Exception e) {
            log.error("Error processing Paystack webhook", e);
        }
    }
}
