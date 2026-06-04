package com.schoolsaas.service;

import com.schoolsaas.dto.payment.InitiatePaymentRequest;
import com.schoolsaas.dto.payment.PaymentResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.model.Payment;
import com.schoolsaas.model.PaymentGatewayType;
import com.schoolsaas.model.SchoolPaymentGatewayConfig;
import com.schoolsaas.repository.PaymentRepository;
import com.schoolsaas.repository.SchoolPaymentGatewayConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

    private final SchoolPaymentGatewayConfigRepository configRepository;
    private final PaymentRepository paymentRepository;
    private final PaystackProvider paystackProvider;
    private final FlutterwaveProvider flutterwaveProvider;

    @Transactional
    public SchoolPaymentGatewayConfig getOrCreateConfig(UUID schoolId) {
        return configRepository.findBySchoolId(schoolId)
                .orElseGet(() -> {
                    SchoolPaymentGatewayConfig defaultConfig = SchoolPaymentGatewayConfig.builder()
                            .schoolId(schoolId)
                            .activeGateway(PaymentGatewayType.PAYSTACK)
                            .fallbackEnabled(false)
                            .paystackEnabled(false)
                            .flutterwaveEnabled(false)
                            .build();
                    return configRepository.save(defaultConfig);
                });
    }

    @Transactional
    public PaymentResponse initiatePayment(UUID schoolId, InitiatePaymentRequest request) {
        SchoolPaymentGatewayConfig config = getOrCreateConfig(schoolId);

        PaymentGatewayType primary = config.getActiveGateway();
        PaymentGatewayType fallback = primary == PaymentGatewayType.PAYSTACK ? PaymentGatewayType.FLUTTERWAVE : PaymentGatewayType.PAYSTACK;

        // Try primary
        PaymentResponse response = tryInitiate(primary, schoolId, request, config);
        if (response != null) {
            return response;
        }

        // Fallback if enabled
        if (Boolean.TRUE.equals(config.getFallbackEnabled())) {
            log.warn("Primary gateway {} failed for school {}, attempting fallback {}", primary, schoolId, fallback);
            PaymentResponse fallbackResponse = tryInitiate(fallback, schoolId, request, config);
            if (fallbackResponse != null) {
                return fallbackResponse;
            }
        }

        throw new BadRequestException("Payment initialization failed on all configured gateways.");
    }

    private PaymentResponse tryInitiate(PaymentGatewayType type, UUID schoolId, InitiatePaymentRequest request, SchoolPaymentGatewayConfig config) {
        try {
            if (type == PaymentGatewayType.PAYSTACK && Boolean.TRUE.equals(config.getPaystackEnabled())) {
                return paystackProvider.initiatePayment(schoolId, request, config);
            }
            if (type == PaymentGatewayType.FLUTTERWAVE && Boolean.TRUE.equals(config.getFlutterwaveEnabled())) {
                return flutterwaveProvider.initiatePayment(schoolId, request, config);
            }
            return null;
        } catch (Exception e) {
            log.warn("Gateway {} initiation failed: {}", type, e.getMessage());
            return null;
        }
    }

    @Transactional
    public PaymentResponse verifyPayment(String reference) {
        Payment payment = paymentRepository.findByPaymentReference(reference)
                .orElseThrow(() -> new BadRequestException("Payment not found"));

        SchoolPaymentGatewayConfig config = getOrCreateConfig(payment.getSchoolId());

        PaymentGatewayType gateway = payment.getGateway() != null ? payment.getGateway() : PaymentGatewayType.PAYSTACK;

        if (gateway == PaymentGatewayType.FLUTTERWAVE) {
            return flutterwaveProvider.verifyPayment(reference, config);
        }
        return paystackProvider.verifyPayment(reference, config);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(UUID schoolId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return paymentRepository.findBySchoolIdAndStatus(schoolId, status, pageable)
                    .map(PaymentResponse::fromEntity);
        }
        return paymentRepository.findBySchoolId(schoolId, pageable)
                .map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getStudentPayments(UUID studentId, Pageable pageable) {
        return paymentRepository.findByStudentId(studentId, pageable)
                .map(PaymentResponse::fromEntity);
    }

    @Transactional
    public void handleWebhook(String payload) {
        // Try to determine gateway from payload structure
        boolean handled = false;

        // Paystack uses "event" and "data.reference"
        if (payload.contains("charge.success") || payload.contains("paystack")) {
            try {
                // We don't know schoolId from Paystack webhook unless we parse it.
                // For simplicity, try to parse reference and look up payment.
                String reference = extractJsonField(payload, "reference");
                if (reference != null && reference.startsWith("PAY_")) {
                    Payment payment = paymentRepository.findByPaymentReference(reference).orElse(null);
                    if (payment != null) {
                        SchoolPaymentGatewayConfig config = getOrCreateConfig(payment.getSchoolId());
                        paystackProvider.handleWebhook(payload, config);
                        handled = true;
                    }
                }
            } catch (Exception e) {
                log.warn("Paystack webhook handling failed", e);
            }
        }

        // Flutterwave uses "event" : "charge.completed" and "data.tx_ref"
        if (!handled && (payload.contains("charge.completed") || payload.contains("flw_ref"))) {
            try {
                String reference = extractJsonField(payload, "tx_ref");
                if (reference != null && reference.startsWith("FLW_")) {
                    Payment payment = paymentRepository.findByPaymentReference(reference).orElse(null);
                    if (payment != null) {
                        SchoolPaymentGatewayConfig config = getOrCreateConfig(payment.getSchoolId());
                        flutterwaveProvider.handleWebhook(payload, config);
                        handled = true;
                    }
                }
            } catch (Exception e) {
                log.warn("Flutterwave webhook handling failed", e);
            }
        }

        if (!handled) {
            log.warn("Webhook could not be matched to any gateway or payment.");
        }
    }

    private String extractJsonField(String json, String fieldName) {
        String search = "\"" + fieldName + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return null;
        int startQuote = json.indexOf("\"", colon);
        if (startQuote == -1) return null;
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}
