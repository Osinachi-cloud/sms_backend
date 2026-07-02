package com.schoolsaas.dto.payment;

import com.schoolsaas.model.PaymentGatewayType;
import com.schoolsaas.model.SchoolPaymentGatewayConfig;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentGatewayConfigResponse {

    private UUID id;
    private UUID schoolId;
    private String paystackPublicKey;
    private String flutterwavePublicKey;
    private PaymentGatewayType activeGateway;
    private Boolean fallbackEnabled;
    private Boolean paystackEnabled;
    private Boolean flutterwaveEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentGatewayConfigResponse fromEntity(SchoolPaymentGatewayConfig config) {
        if (config == null) return null;
        return PaymentGatewayConfigResponse.builder()
                .id(config.getId())
                .schoolId(config.getSchoolId())
                .paystackPublicKey(config.getPaystackPublicKey())
                .flutterwavePublicKey(config.getFlutterwavePublicKey())
                .activeGateway(config.getActiveGateway())
                .fallbackEnabled(config.getFallbackEnabled())
                .paystackEnabled(config.getPaystackEnabled())
                .flutterwaveEnabled(config.getFlutterwaveEnabled())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
