package com.schoolsaas.dto.payment;

import com.schoolsaas.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paymentReference;
    private String paystackReference;
    private String paystackAccessCode;
    private String authorizationUrl;
    private String status;
    private LocalDateTime paidAt;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .studentId(payment.getStudentId())
                .studentName(payment.getStudent() != null ? payment.getStudent().getFullName() : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .paymentReference(payment.getPaymentReference())
                .paystackReference(payment.getPaystackReference())
                .paystackAccessCode(payment.getPaystackAccessCode())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .metadata(payment.getMetadata())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
