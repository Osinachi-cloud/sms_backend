package com.schoolsaas.repository;

import com.schoolsaas.model.EntityDeletionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntityDeletionRequestRepository extends JpaRepository<EntityDeletionRequest, UUID> {

    Page<EntityDeletionRequest> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    Page<EntityDeletionRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<EntityDeletionRequest> findBySchoolIdAndStatusOrderByCreatedAtDesc(UUID schoolId, String status, Pageable pageable);

    List<EntityDeletionRequest> findBySchoolIdAndEntityTypeAndEntityIdAndStatus(UUID schoolId, String entityType, UUID entityId, String status);

    boolean existsBySchoolIdAndEntityTypeAndEntityIdAndStatus(UUID schoolId, String entityType, UUID entityId, String status);
}
