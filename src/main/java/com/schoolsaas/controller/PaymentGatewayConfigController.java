package com.schoolsaas.controller;

import com.schoolsaas.dto.payment.PaymentGatewayConfigRequest;
import com.schoolsaas.dto.payment.PaymentGatewayConfigResponse;
import com.schoolsaas.service.PaymentGatewayConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/payment-gateway-config")
@RequiredArgsConstructor
public class PaymentGatewayConfigController {

    private final PaymentGatewayConfigService configService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentGatewayConfigResponse> getConfig(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(configService.getConfig(schoolId));
    }

    @PutMapping
    @PreAuthorize("hasPermission(#schoolId, 'payment.gateway.manage') or hasPermission(#schoolId, 'payment.gateway.switch') or hasRole('APP_ADMIN')")
    public ResponseEntity<PaymentGatewayConfigResponse> updateConfig(
            @PathVariable UUID schoolId,
            @Valid @RequestBody PaymentGatewayConfigRequest request) {
        return ResponseEntity.ok(configService.updateConfig(schoolId, request));
    }
}
