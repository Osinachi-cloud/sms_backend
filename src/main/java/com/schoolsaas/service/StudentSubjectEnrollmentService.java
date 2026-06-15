package com.schoolsaas.service;

import com.schoolsaas.dto.payment.InitiatePaymentRequest;
import com.schoolsaas.dto.payment.PaymentResponse;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentSubjectEnrollmentService {

    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;

    @Transactional(readOnly = true)
    public List<StudentSubjectEnrollment> getStudentEnrollments(UUID schoolId, UUID studentId) {
        return enrollmentRepository.findBySchoolIdAndStudentId(schoolId, studentId);
    }

    @Transactional(readOnly = true)
    public List<StudentSubjectEnrollment> getSubjectEnrollments(UUID schoolId, UUID subjectId) {
        return enrollmentRepository.findBySchoolIdAndSubjectId(schoolId, subjectId);
    }

    @Transactional
    public StudentSubjectEnrollment enrollStudent(UUID schoolId, UUID studentId, UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
        if (!subject.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Subject", "id", subjectId);
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        if (!student.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        if (enrollmentRepository.existsBySchoolIdAndStudentIdAndSubjectId(schoolId, studentId, subjectId)) {
            throw new BadRequestException("Student is already enrolled in this subject");
        }

        if (Boolean.FALSE.equals(subject.getIsFree()) && subject.getCost() != null && subject.getCost().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("This subject requires payment before enrollment");
        }

        StudentSubjectEnrollment enrollment = StudentSubjectEnrollment.builder()
                .schoolId(schoolId)
                .studentId(studentId)
                .subjectId(subjectId)
                .status("ENROLLED")
                .build();
        return enrollmentRepository.save(enrollment);
    }

    @Transactional
    public PaymentResponse initiateSubjectPayment(UUID schoolId, UUID studentId, UUID subjectId, String callbackUrl) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
        if (!subject.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Subject", "id", subjectId);
        }
        if (Boolean.TRUE.equals(subject.getIsFree()) || subject.getCost() == null || subject.getCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This subject does not require payment");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setStudentId(studentId);
        request.setAmount(subject.getCost());
        request.setCallbackUrl(callbackUrl);
        request.setSubjectId(subjectId);

        return paymentGatewayService.initiatePayment(schoolId, request);
    }

    @Transactional
    public StudentSubjectEnrollment completeEnrollmentAfterPayment(UUID schoolId, UUID studentId, UUID subjectId, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new BadRequestException("Payment has not been completed successfully");
        }

        if (enrollmentRepository.existsBySchoolIdAndStudentIdAndSubjectId(schoolId, studentId, subjectId)) {
            return enrollmentRepository.findBySchoolIdAndStudentIdAndSubjectId(schoolId, studentId, subjectId).orElse(null);
        }

        StudentSubjectEnrollment enrollment = StudentSubjectEnrollment.builder()
                .schoolId(schoolId)
                .studentId(studentId)
                .subjectId(subjectId)
                .status("ENROLLED")
                .paidAmount(payment.getAmount())
                .paymentId(paymentId)
                .build();
        return enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void dropEnrollment(UUID schoolId, UUID enrollmentId) {
        StudentSubjectEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));
        if (!enrollment.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Enrollment", "id", enrollmentId);
        }
        enrollment.setStatus("DROPPED");
        enrollmentRepository.save(enrollment);
    }
}
