package com.schoolsaas.service;

import com.schoolsaas.model.Payment;
import com.schoolsaas.model.StudentSubjectEnrollment;
import com.schoolsaas.repository.PaymentRepository;
import com.schoolsaas.repository.StudentSubjectEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompletionService {

    private final PaymentRepository paymentRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;

    @Transactional
    public void handlePaymentSuccess(UUID paymentReferenceId) {
        Payment payment = paymentRepository.findById(paymentReferenceId).orElse(null);
        if (payment == null || !"SUCCESS".equals(payment.getStatus())) {
            return;
        }

        Map<String, Object> metadata = payment.getMetadata();
        Object subjectIdObj = metadata != null ? metadata.get("subject_id") : null;
        if (subjectIdObj == null && metadata != null) {
            subjectIdObj = metadata.get("subjectId");
        }

        if (subjectIdObj == null || payment.getStudentId() == null || payment.getSchoolId() == null) {
            return;
        }

        try {
            UUID schoolId = payment.getSchoolId();
            UUID studentId = payment.getStudentId();
            UUID subjectId = UUID.fromString(subjectIdObj.toString());

            if (!enrollmentRepository.existsBySchoolIdAndStudentIdAndSubjectId(schoolId, studentId, subjectId)) {
                enrollmentRepository.save(StudentSubjectEnrollment.builder()
                        .schoolId(schoolId)
                        .studentId(studentId)
                        .subjectId(subjectId)
                        .status("ENROLLED")
                        .paidAmount(payment.getAmount())
                        .paymentId(payment.getId())
                        .build());
                log.info("Auto-enrolled student {} to subject {} after payment {}", studentId, subjectId, payment.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to auto-enroll after payment: {}", e.getMessage());
        }
    }
}
