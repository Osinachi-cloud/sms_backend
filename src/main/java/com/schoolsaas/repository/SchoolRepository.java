package com.schoolsaas.repository;

import com.schoolsaas.model.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {

    Optional<School> findBySubdomain(String subdomain);

    Optional<School> findByCode(String code);

    boolean existsBySubdomain(String subdomain);

    boolean existsByCode(String code);

    @Query("SELECT s FROM School s WHERE s.status = :status")
    Page<School> findByStatus(String status, Pageable pageable);

    @Query("SELECT s FROM School s WHERE s.status != 'DELETED'")
    Page<School> findAllActive(Pageable pageable);

    @Query("SELECT s FROM School s WHERE s.status = 'ACTIVE' AND " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<School> searchByNameOrCode(String search, Pageable pageable);
}
