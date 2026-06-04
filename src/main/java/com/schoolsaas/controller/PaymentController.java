package com.schoolsaas.controller;

import com.schoolsaas.dto.payment.InitiatePaymentRequest;
import com.schoolsaas.dto.payment.PaymentResponse;
import com.schoolsaas.service.PaymentGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentGatewayService paymentGatewayService;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'payment.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(paymentGatewayService.getPayments(schoolId, status, pageable));
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasPermission(#schoolId, 'payment.create')")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @PathVariable UUID schoolId,
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(paymentGatewayService.initiatePayment(schoolId, request));
    }

    @GetMapping("/verify/{reference}")
    @PreAuthorize("hasPermission(#schoolId, 'payment.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @PathVariable UUID schoolId,
            @PathVariable String reference) {
        return ResponseEntity.ok(paymentGatewayService.verifyPayment(reference));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasPermission(#schoolId, 'payment.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getStudentPayments(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            Pageable pageable) {
        return ResponseEntity.ok(paymentGatewayService.getStudentPayments(studentId, pageable));
    }
}
