package com.schoolsaas.dto.payment;

import com.schoolsaas.model.PaymentGatewayType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentGatewayConfigRequest {

    private String paystackSecretKey;
    private String paystackPublicKey;
    private String flutterwaveSecretKey;
    private String flutterwavePublicKey;

    @NotNull
    private PaymentGatewayType activeGateway;

    @NotNull
    private Boolean fallbackEnabled;

    @NotNull
    private Boolean paystackEnabled;

    @NotNull
    private Boolean flutterwaveEnabled;
}
