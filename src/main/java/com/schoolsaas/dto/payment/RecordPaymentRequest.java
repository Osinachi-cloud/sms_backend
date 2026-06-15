package com.schoolsaas.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RecordPaymentRequest {
    @NotNull(message = "Student ID is required")
    private UUID studentId;

    private UUID studentFeeId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String currency = "NGN";
    private String paymentMethod;
    private String description;
}
