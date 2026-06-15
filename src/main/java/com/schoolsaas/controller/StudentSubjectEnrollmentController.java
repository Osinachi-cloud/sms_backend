package com.schoolsaas.controller;

import com.schoolsaas.dto.payment.PaymentResponse;
import com.schoolsaas.model.StudentSubjectEnrollment;
import com.schoolsaas.service.StudentSubjectEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/enrollments")
@RequiredArgsConstructor
public class StudentSubjectEnrollmentController {

    private final StudentSubjectEnrollmentService enrollmentService;

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasPermission(#schoolId, 'student.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN') or isAuthenticated()")
    public ResponseEntity<Page<StudentSubjectEnrollment>> getStudentEnrollments(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            Pageable pageable) {
        List<StudentSubjectEnrollment> list = enrollmentService.getStudentEnrollments(schoolId, studentId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @GetMapping("/subjects/{subjectId}")
    @PreAuthorize("hasPermission(#schoolId, 'subject.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<StudentSubjectEnrollment>> getSubjectEnrollments(
            @PathVariable UUID schoolId,
            @PathVariable UUID subjectId,
            Pageable pageable) {
        List<StudentSubjectEnrollment> list = enrollmentService.getSubjectEnrollments(schoolId, subjectId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @PostMapping("/students/{studentId}/subjects/{subjectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StudentSubjectEnrollment> enrollStudent(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @PathVariable UUID subjectId) {
        return ResponseEntity.ok(enrollmentService.enrollStudent(schoolId, studentId, subjectId));
    }

    @PostMapping("/students/{studentId}/subjects/{subjectId}/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> initiateSubjectPayment(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @PathVariable UUID subjectId,
            @RequestBody Map<String, String> body) {
        String callbackUrl = body.get("callbackUrl");
        return ResponseEntity.ok(enrollmentService.initiateSubjectPayment(schoolId, studentId, subjectId, callbackUrl));
    }

    @PostMapping("/students/{studentId}/subjects/{subjectId}/confirm-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StudentSubjectEnrollment> confirmPaymentEnrollment(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @PathVariable UUID subjectId,
            @RequestBody Map<String, String> body) {
        UUID paymentId = UUID.fromString(body.get("paymentId"));
        return ResponseEntity.ok(enrollmentService.completeEnrollmentAfterPayment(schoolId, studentId, subjectId, paymentId));
    }

    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasPermission(#schoolId, 'subject.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> dropEnrollment(
            @PathVariable UUID schoolId,
            @PathVariable UUID enrollmentId) {
        enrollmentService.dropEnrollment(schoolId, enrollmentId);
        return ResponseEntity.ok().build();
    }
}
