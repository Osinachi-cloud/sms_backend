package com.schoolsaas.repository;

import com.schoolsaas.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    List<UserBadge> findByUserIdAndSchoolId(UUID userId, UUID schoolId);
    boolean existsByUserIdAndBadgeIdAndSchoolId(UUID userId, UUID badgeId, UUID schoolId);
}
