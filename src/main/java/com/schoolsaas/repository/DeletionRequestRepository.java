package com.schoolsaas.repository;

import com.schoolsaas.model.DeletionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeletionRequestRepository extends JpaRepository<DeletionRequest, UUID> {

    Page<DeletionRequest> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    Page<DeletionRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<DeletionRequest> findByStatusInOrderByCreatedAtDesc(List<String> statuses, Pageable pageable);

    Optional<DeletionRequest> findBySchoolIdAndStatusIn(UUID schoolId, List<String> statuses);

    @Query("SELECT d FROM DeletionRequest d WHERE d.status = :status")
    Page<DeletionRequest> findPendingRequests(String status, Pageable pageable);

    boolean existsBySchoolIdAndStatusIn(UUID schoolId, List<String> statuses);
}
