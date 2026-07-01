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
public class FlutterwaveProvider implements PaymentGatewayProvider {

    @Value("${flutterwave.secret-key:}")
    private String globalSecretKey;

    @Value("${flutterwave.base-url:https://api.flutterwave.com/v3}")
    private String baseUrl;

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getGatewayName() {
        return "FLUTTERWAVE";
    }

    private String resolveSecretKey(SchoolPaymentGatewayConfig config) {
        if (config != null && config.getFlutterwaveSecretKey() != null && !config.getFlutterwaveSecretKey().isBlank()) {
            return config.getFlutterwaveSecretKey();
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

        String reference = "FLW_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        Payment payment = Payment.builder()
                .schoolId(schoolId)
                .studentId(request.getStudentId())
                .studentFeeId(request.getStudentFeeId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "NGN")
                .paymentReference(reference)
                .gateway(PaymentGatewayType.FLUTTERWAVE)
                .status("PENDING")
                .build();

        String secretKey = resolveSecretKey(config);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(secretKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("tx_ref", reference);
            body.put("amount", request.getAmount().toPlainString());
            body.put("currency", payment.getCurrency());
            body.put("redirect_url", request.getCallbackUrl() != null ? request.getCallbackUrl() : "");

            Map<String, String> customer = new HashMap<>();
            customer.put("email", student.getEmail() != null ? student.getEmail() : student.getParentEmail());
            customer.put("name", student.getFullName());
            body.put("customer", customer);

            Map<String, String> meta = new HashMap<>();
            meta.put("school_id", schoolId.toString());
            meta.put("student_id", student.getId().toString());
            meta.put("student_name", student.getFullName());
            if (request.getSubjectId() != null) {
                meta.put("subject_id", request.getSubjectId().toString());
            }
            if (request.getDescription() != null) {
                meta.put("description", request.getDescription());
            }
            body.put("meta", meta);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/payments",
                    entity,
                    String.class
            );

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            if (jsonResponse.get("status").asText().equalsIgnoreCase("success")) {
                JsonNode data = jsonResponse.get("data");
                payment.setPaystackReference(data.has("id") ? data.get("id").asText() : null);

                payment = paymentRepository.save(payment);

                PaymentResponse paymentResponse = PaymentResponse.fromEntity(payment);
                paymentResponse.setAuthorizationUrl(data.get("link").asText());
                return paymentResponse;
            } else {
                throw new BadRequestException("Flutterwave payment initialization failed: " + jsonResponse.get("message").asText());
            }

        } catch (Exception e) {
            log.error("Error initializing Flutterwave payment", e);
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
                    baseUrl + "/transactions/verify_by_reference?tx_ref=" + reference,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            if (jsonResponse.get("status").asText().equalsIgnoreCase("success")) {
                JsonNode data = jsonResponse.get("data");
                String flwStatus = data.get("status").asText();

                if ("successful".equalsIgnoreCase(flwStatus)) {
                    payment.setStatus("SUCCESS");
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setPaymentMethod(data.has("payment_type") ? data.get("payment_type").asText() : "card");
                } else if ("failed".equalsIgnoreCase(flwStatus)) {
                    payment.setStatus("FAILED");
                } else {
                    payment.setStatus("PENDING");
                }

                Map<String, Object> metadata = new HashMap<>();
                if (data.has("meta")) {
                    metadata = objectMapper.convertValue(data.get("meta"), Map.class);
                }
                metadata.put("gateway_response", data.has("processor_response") ? data.get("processor_response").asText() : null);
                metadata.put("flw_ref", data.has("flw_ref") ? data.get("flw_ref").asText() : null);
                payment.setMetadata(metadata);

                payment = paymentRepository.save(payment);
                log.info("Flutterwave payment verified: {} status: {}", reference, payment.getStatus());
            }

        } catch (Exception e) {
            log.error("Error verifying Flutterwave payment", e);
            throw new BadRequestException("Payment verification failed: " + e.getMessage());
        }

        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, SchoolPaymentGatewayConfig config) {
        log.info("Received Flutterwave webhook");
        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.get("event").asText();

            if ("charge.completed".equals(eventType)) {
                JsonNode data = event.get("data");
                String reference = data.get("tx_ref").asText();
                verifyPayment(reference, config);
            }
        } catch (Exception e) {
            log.error("Error processing Flutterwave webhook", e);
        }
    }
}
