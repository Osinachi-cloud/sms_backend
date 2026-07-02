package com.schoolsaas.controller;

import com.schoolsaas.dto.payment.InitiatePaymentRequest;
import com.schoolsaas.dto.payment.PaymentResponse;
import com.schoolsaas.dto.payment.RecordPaymentRequest;
import com.schoolsaas.model.Parent;
import com.schoolsaas.model.ParentStudent;
import com.schoolsaas.model.Student;
import com.schoolsaas.repository.ParentRepository;
import com.schoolsaas.repository.ParentStudentRepository;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.service.PaymentGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schools/{schoolId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentGatewayService paymentGatewayService;
    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @PathVariable UUID schoolId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(paymentGatewayService.getPayments(schoolId, status, pageable));
    }

    @PostMapping("/initialize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @PathVariable UUID schoolId,
            @Valid @RequestBody InitiatePaymentRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        // Admins with payment.create can initiate for anyone
        if (SecurityUtils.hasPermission("payment.create")) {
            return ResponseEntity.ok(paymentGatewayService.initiatePayment(schoolId, request));
        }

        // Students can initiate payment for themselves
        Optional<Student> studentOpt = studentRepository.findByUserId(currentUserId);
        if (studentOpt.isPresent() && studentOpt.get().getId().equals(request.getStudentId())) {
            return ResponseEntity.ok(paymentGatewayService.initiatePayment(schoolId, request));
        }

        // Parents can initiate payment for their children
        var parentOpt = parentRepository.findByUserIdAndSchoolId(currentUserId, schoolId);
        if (parentOpt.isPresent()) {
            Parent parent = parentOpt.get();
            List<UUID> childIds = parentStudentRepository.findByParentId(parent.getId()).stream()
                    .map(ParentStudent::getStudentId)
                    .collect(Collectors.toList());
            if (childIds.contains(request.getStudentId())) {
                return ResponseEntity.ok(paymentGatewayService.initiatePayment(schoolId, request));
            }
        }

        return ResponseEntity.status(403).build();
    }

    @PostMapping("/record")
    @PreAuthorize("hasPermission(#schoolId, 'payment.create') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable UUID schoolId,
            @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(paymentGatewayService.recordPayment(schoolId, request));
    }

    @GetMapping("/verify/{reference}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @PathVariable UUID schoolId,
            @PathVariable String reference) {
        return ResponseEntity.ok(paymentGatewayService.verifyPayment(reference));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentResponse>> getStudentPayments(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            Pageable pageable) {
        return ResponseEntity.ok(paymentGatewayService.getStudentPayments(studentId, pageable));
    }

    @GetMapping("/parent-view/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentResponse>> getParentViewOfStudentPayments(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        var parentOpt = parentRepository.findByUserIdAndSchoolId(userId, schoolId);
        if (parentOpt.isEmpty()) {
            return ResponseEntity.ok(Page.empty());
        }
        Parent parent = parentOpt.get();
        List<UUID> childIds = parentStudentRepository.findByParentId(parent.getId()).stream()
                .map(ParentStudent::getStudentId)
                .collect(Collectors.toList());
        if (!childIds.contains(studentId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(paymentGatewayService.getStudentPayments(studentId, pageable));
    }
}
