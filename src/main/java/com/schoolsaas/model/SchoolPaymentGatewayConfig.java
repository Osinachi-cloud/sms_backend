package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "school_payment_gateway_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolPaymentGatewayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false, unique = true)
    private UUID schoolId;

    @Column(name = "paystack_secret_key")
    private String paystackSecretKey;

    @Column(name = "paystack_public_key")
    private String paystackPublicKey;

    @Column(name = "flutterwave_secret_key")
    private String flutterwaveSecretKey;

    @Column(name = "flutterwave_public_key")
    private String flutterwavePublicKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_gateway", nullable = false, length = 20)
    @Builder.Default
    private PaymentGatewayType activeGateway = PaymentGatewayType.PAYSTACK;

    @Column(name = "fallback_enabled", nullable = false)
    @Builder.Default
    private Boolean fallbackEnabled = false;

    @Column(name = "paystack_enabled", nullable = false)
    @Builder.Default
    private Boolean paystackEnabled = false;

    @Column(name = "flutterwave_enabled", nullable = false)
    @Builder.Default
    private Boolean flutterwaveEnabled = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
