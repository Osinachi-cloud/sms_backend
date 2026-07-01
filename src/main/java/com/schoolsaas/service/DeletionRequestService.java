package com.schoolsaas.service;

import tools.jackson.databind.ObjectMapper;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletionRequestService {

    private final DeletionRequestRepository deletionRequestRepository;
    private final SchoolBackupRepository backupRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final ContentItemRepository contentItemRepository;
    private final PaymentRepository paymentRepository;
    private final UserSchoolRepository userSchoolRepository;
    private final ObjectMapper objectMapper;

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "REVIEWED", "FORWARDED");

    @Transactional
    public DeletionRequest createRequest(UUID schoolId, UUID requestedBy, String reason) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        if (deletionRequestRepository.existsBySchoolIdAndStatusIn(schoolId, ACTIVE_STATUSES)) {
            throw new BadRequestException("A deletion request for this school is already in progress");
        }

        DeletionRequest request = DeletionRequest.builder()
                .schoolId(schoolId)
                .requestedBy(requestedBy)
                .reason(reason)
                .status("PENDING")
                .build();

        DeletionRequest saved = deletionRequestRepository.save(request);
        log.info("Deletion request created for school {} by user {}", schoolId, requestedBy);

        return saved;
    }

    @Transactional
    public DeletionRequest reviewRequest(UUID requestId, UUID reviewedBy, String notes, boolean approve) {
        DeletionRequest request = deletionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("DeletionRequest", "id", requestId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new BadRequestException("Request is not in PENDING status");
        }

        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNotes(notes);
        request.setStatus(approve ? "REVIEWED" : "REJECTED");

        log.info("Deletion request {} reviewed by {} - {}", requestId, reviewedBy, approve ? "APPROVED" : "REJECTED");

        return deletionRequestRepository.save(request);
    }

    @Transactional
    public DeletionRequest forwardRequest(UUID requestId, UUID forwardedBy, String notes) {
        DeletionRequest request = deletionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("DeletionRequest", "id", requestId));

        if (!"REVIEWED".equals(request.getStatus())) {
            throw new BadRequestException("Request must be REVIEWED before forwarding");
        }

        request.setForwardedBy(forwardedBy);
        request.setForwardedAt(LocalDateTime.now());
        request.setForwardNotes(notes);
        request.setStatus("FORWARDED");

        log.info("Deletion request {} forwarded by {}", requestId, forwardedBy);

        return deletionRequestRepository.save(request);
    }

    @Transactional
    public DeletionRequest finalDecision(UUID requestId, UUID decidedBy, String notes, boolean approve) {
        DeletionRequest request = deletionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("DeletionRequest", "id", requestId));

        if (!"FORWARDED".equals(request.getStatus())) {
            throw new BadRequestException("Request must be FORWARDED before final decision");
        }

        request.setDecidedBy(decidedBy);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecisionNotes(notes);
        request.setStatus(approve ? "APPROVED" : "REJECTED");

        if (approve) {
            createBackup(request.getSchoolId(), request.getId());
            softDeleteSchool(request.getSchoolId());
            log.info("School {} has been soft-deleted after deletion request {} was approved",
                    request.getSchoolId(), requestId);
        }

        return deletionRequestRepository.save(request);
    }

    @Transactional
    protected void createBackup(UUID schoolId, UUID deletionRequestId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        Map<String, Object> schoolData = objectMapper.convertValue(school, Map.class);

        List<Student> students = studentRepository.findBySchoolId(schoolId);
        Map<String, Object> studentsData = Map.of("students", objectMapper.convertValue(students, List.class));

        List<Teacher> teachers = teacherRepository.findBySchoolId(schoolId);
        Map<String, Object> teachersData = Map.of("teachers", objectMapper.convertValue(teachers, List.class));

        List<SchoolClass> classes = classRepository.findBySchoolId(schoolId);
        Map<String, Object> classesData = Map.of("classes", objectMapper.convertValue(classes, List.class));

        List<ContentItem> content = contentItemRepository.findBySchoolId(schoolId);
        Map<String, Object> contentData = Map.of("content", objectMapper.convertValue(content, List.class));

        List<Payment> payments = paymentRepository.findBySchoolId(schoolId);
        Map<String, Object> paymentsData = Map.of("payments", objectMapper.convertValue(payments, List.class));

        SchoolBackup backup = SchoolBackup.builder()
                .schoolId(schoolId)
                .schoolData(schoolData)
                .studentsData(studentsData)
                .teachersData(teachersData)
                .classesData(classesData)
                .contentData(contentData)
                .paymentsData(paymentsData)
                .deletionRequestId(deletionRequestId)
                .build();

        backupRepository.save(backup);
        log.info("Backup created for school {}", schoolId);
    }

    @Transactional
    protected void softDeleteSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", "id", schoolId));

        school.setStatus("DELETED");
        schoolRepository.save(school);

        userSchoolRepository.findBySchoolId(schoolId).forEach(userSchool -> {
            userSchool.setIsActive(false);
            userSchoolRepository.save(userSchool);
        });
    }

    @Transactional(readOnly = true)
    public Page<DeletionRequest> getPendingRequests(Pageable pageable) {
        return deletionRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING", pageable);
    }

    @Transactional(readOnly = true)
    public Page<DeletionRequest> getReviewedRequests(Pageable pageable) {
        return deletionRequestRepository.findByStatusOrderByCreatedAtDesc("REVIEWED", pageable);
    }

    @Transactional(readOnly = true)
    public Page<DeletionRequest> getForwardedRequests(Pageable pageable) {
        return deletionRequestRepository.findByStatusOrderByCreatedAtDesc("FORWARDED", pageable);
    }

    @Transactional(readOnly = true)
    public Page<DeletionRequest> getAllRequests(Pageable pageable) {
        return deletionRequestRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<DeletionRequest> getRequestsBySchool(UUID schoolId, Pageable pageable) {
        return deletionRequestRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable);
    }

    @Transactional(readOnly = true)
    public DeletionRequest getRequest(UUID requestId) {
        return deletionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("DeletionRequest", "id", requestId));
    }
}
