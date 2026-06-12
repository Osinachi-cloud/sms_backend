package com.schoolsaas.service;

import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.EntityDeletionRequest;
import com.schoolsaas.model.Student;
import com.schoolsaas.model.Teacher;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityDeletionRequestService {

    private final EntityDeletionRequestRepository requestRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final UserSchoolRepository userSchoolRepository;

    @Transactional
    public EntityDeletionRequest createRequest(UUID schoolId, UUID requestedBy, String entityType, UUID entityId, String reason) {
        // Check for existing pending request
        if (requestRepository.existsBySchoolIdAndEntityTypeAndEntityIdAndStatus(schoolId, entityType, entityId, "PENDING")) {
            throw new BadRequestException("A deletion request for this record is already pending approval");
        }

        String entityName = resolveEntityName(schoolId, entityType, entityId);

        EntityDeletionRequest request = EntityDeletionRequest.builder()
                .schoolId(schoolId)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .requestedBy(requestedBy)
                .reason(reason)
                .status("PENDING")
                .build();

        EntityDeletionRequest saved = requestRepository.save(request);
        log.info("Entity deletion request created: {} {} by user {}", entityType, entityId, requestedBy);
        return saved;
    }

    @Transactional
    public EntityDeletionRequest reviewRequest(UUID requestId, UUID reviewedBy, String notes, boolean approve) {
        EntityDeletionRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityDeletionRequest", "id", requestId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new BadRequestException("Request is not in PENDING status");
        }

        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNotes(notes);
        request.setStatus(approve ? "APPROVED" : "REJECTED");

        if (approve) {
            performDeletion(request);
        }

        log.info("Entity deletion request {} {} by {}", requestId, approve ? "APPROVED" : "REJECTED", reviewedBy);
        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Page<EntityDeletionRequest> getPendingRequests(Pageable pageable) {
        return requestRepository.findByStatusOrderByCreatedAtDesc("PENDING", pageable);
    }

    @Transactional(readOnly = true)
    public Page<EntityDeletionRequest> getAllRequests(Pageable pageable) {
        return requestRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<EntityDeletionRequest> getRequestsBySchool(UUID schoolId, Pageable pageable) {
        return requestRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable);
    }

    @Transactional(readOnly = true)
    public EntityDeletionRequest getRequest(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityDeletionRequest", "id", requestId));
    }

    private void performDeletion(EntityDeletionRequest request) {
        try {
            switch (request.getEntityType()) {
                case "STUDENT" -> {
                    Student student = studentRepository.findById(request.getEntityId()).orElse(null);
                    if (student != null) {
                        student.setStatus("INACTIVE");
                        studentRepository.save(student);
                    }
                }
                case "TEACHER" -> {
                    Teacher teacher = teacherRepository.findById(request.getEntityId()).orElse(null);
                    if (teacher != null) {
                        teacher.setStatus("INACTIVE");
                        teacherRepository.save(teacher);
                    }
                }
                case "CLASS" -> {
                    var schoolClass = classRepository.findById(request.getEntityId()).orElse(null);
                    if (schoolClass != null) {
                        schoolClass.setIsActive(false);
                        classRepository.save(schoolClass);
                    }
                }
                case "USER" -> {
                    var user = userRepository.findById(request.getEntityId()).orElse(null);
                    if (user != null) {
                        user.setIsActive(false);
                        userRepository.save(user);
                        // Also deactivate school membership
                        userSchoolRepository.findByUserId(request.getEntityId())
                                .forEach(us -> {
                                    us.setIsActive(false);
                                    userSchoolRepository.save(us);
                                });
                    }
                }
            }
            log.info("Entity {} {} soft-deleted after approval", request.getEntityType(), request.getEntityId());
        } catch (Exception e) {
            log.error("Failed to delete entity {} {} after approval", request.getEntityType(), request.getEntityId(), e);
            throw new BadRequestException("Failed to delete entity after approval: " + e.getMessage());
        }
    }

    private String resolveEntityName(UUID schoolId, String entityType, UUID entityId) {
        try {
            return switch (entityType) {
                case "STUDENT" -> studentRepository.findById(entityId).map(Student::getFullName).orElse(null);
                case "TEACHER" -> teacherRepository.findById(entityId).map(Teacher::getFullName).orElse(null);
                case "CLASS" -> classRepository.findById(entityId).map(c -> c.getName() + (c.getSection() != null ? " (" + c.getSection() + ")" : "")).orElse(null);
                case "USER" -> userRepository.findById(entityId).map(u -> u.getFullName() + " (" + u.getEmail() + ")").orElse(null);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }
}
