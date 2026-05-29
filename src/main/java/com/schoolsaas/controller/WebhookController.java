package com.schoolsaas.controller;

import com.schoolsaas.service.PaystackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaystackService paystackService;

    @PostMapping("/paystack")
    public ResponseEntity<Void> paystackWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {
        paystackService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
