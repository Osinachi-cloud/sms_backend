package com.schoolsaas.repository;

import com.schoolsaas.model.Parent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {
    Page<Parent> findBySchoolId(UUID schoolId, Pageable pageable);

    List<Parent> findBySchoolIdAndIsActiveTrue(UUID schoolId);

    Page<Parent> findBySchoolIdAndIsActiveTrue(UUID schoolId, Pageable pageable);

    Optional<Parent> findByUserId(UUID userId);

    Optional<Parent> findByUserIdAndSchoolId(UUID userId, UUID schoolId);

    @Modifying
    @Query("UPDATE Parent p SET p.email = :newEmail WHERE p.email = :oldEmail")
    int updateEmailByEmail(String oldEmail, String newEmail);
}
