package com.schoolsaas.repository;

import com.schoolsaas.model.PointsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, UUID> {
    List<PointsTransaction> findByUserIdAndSchoolIdOrderByCreatedAtDesc(UUID userId, UUID schoolId);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointsTransaction p WHERE p.userId = :userId AND p.schoolId = :schoolId")
    Integer getTotalPointsByUserAndSchool(UUID userId, UUID schoolId);
}
