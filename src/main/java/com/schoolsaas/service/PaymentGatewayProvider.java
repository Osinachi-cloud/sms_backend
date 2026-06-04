package com.schoolsaas.service;

import com.schoolsaas.dto.payment.InitiatePaymentRequest;
import com.schoolsaas.dto.payment.PaymentResponse;
import com.schoolsaas.model.SchoolPaymentGatewayConfig;

import java.util.UUID;

public interface PaymentGatewayProvider {

    PaymentResponse initiatePayment(UUID schoolId, InitiatePaymentRequest request, SchoolPaymentGatewayConfig config);

    PaymentResponse verifyPayment(String reference, SchoolPaymentGatewayConfig config);

    void handleWebhook(String payload, SchoolPaymentGatewayConfig config);

    String getGatewayName();
}
