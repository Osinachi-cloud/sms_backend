package com.schoolsaas.controller;

import com.schoolsaas.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentGatewayService paymentGatewayService;

    @PostMapping("/paystack")
    public ResponseEntity<Void> paystackWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {
        paymentGatewayService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/flutterwave")
    public ResponseEntity<Void> flutterwaveWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "verif-hash", required = false) String signature) {
        paymentGatewayService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
