package com.schoolsaas.service;

import com.schoolsaas.dto.payment.PaymentGatewayConfigRequest;
import com.schoolsaas.dto.payment.PaymentGatewayConfigResponse;
import com.schoolsaas.model.PaymentGatewayType;
import com.schoolsaas.model.SchoolPaymentGatewayConfig;
import com.schoolsaas.repository.SchoolPaymentGatewayConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentGatewayConfigService {

    private final SchoolPaymentGatewayConfigRepository configRepository;

    @Transactional(readOnly = true)
    public PaymentGatewayConfigResponse getConfig(UUID schoolId) {
        SchoolPaymentGatewayConfig config = configRepository.findBySchoolId(schoolId)
                .orElse(null);
        if (config == null) {
            return PaymentGatewayConfigResponse.builder()
                    .schoolId(schoolId)
                    .activeGateway(PaymentGatewayType.PAYSTACK)
                    .fallbackEnabled(false)
                    .paystackEnabled(false)
                    .flutterwaveEnabled(false)
                    .build();
        }
        return PaymentGatewayConfigResponse.fromEntity(config);
    }

    @Transactional
    public PaymentGatewayConfigResponse updateConfig(UUID schoolId, PaymentGatewayConfigRequest request) {
        SchoolPaymentGatewayConfig config = configRepository.findBySchoolId(schoolId)
                .orElse(SchoolPaymentGatewayConfig.builder().schoolId(schoolId).build());

        config.setPaystackSecretKey(updateKey(config.getPaystackSecretKey(), request.getPaystackSecretKey()));
        config.setPaystackPublicKey(updateKey(config.getPaystackPublicKey(), request.getPaystackPublicKey()));
        config.setFlutterwaveSecretKey(updateKey(config.getFlutterwaveSecretKey(), request.getFlutterwaveSecretKey()));
        config.setFlutterwavePublicKey(updateKey(config.getFlutterwavePublicKey(), request.getFlutterwavePublicKey()));
        config.setActiveGateway(request.getActiveGateway());
        config.setFallbackEnabled(request.getFallbackEnabled());
        config.setPaystackEnabled(request.getPaystackEnabled());
        config.setFlutterwaveEnabled(request.getFlutterwaveEnabled());

        config = configRepository.save(config);
        return PaymentGatewayConfigResponse.fromEntity(config);
    }

    private String updateKey(String existing, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return existing;
        }
        if (incoming.contains("****")) {
            return existing;
        }
        return incoming;
    }
}
