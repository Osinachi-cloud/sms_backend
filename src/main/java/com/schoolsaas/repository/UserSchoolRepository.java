package com.schoolsaas.repository;

import com.schoolsaas.model.UserSchool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSchoolRepository extends JpaRepository<UserSchool, UUID> {

    Optional<UserSchool> findByUserIdAndSchoolId(UUID userId, UUID schoolId);

    List<UserSchool> findByUserId(UUID userId);

    List<UserSchool> findBySchoolId(UUID schoolId);

    Page<UserSchool> findBySchoolIdAndIsActiveTrue(UUID schoolId, Pageable pageable);

    boolean existsByUserIdAndSchoolId(UUID userId, UUID schoolId);

    @Query("SELECT us FROM UserSchool us WHERE us.schoolId = :schoolId AND us.roleId = :roleId AND us.isActive = true")
    List<UserSchool> findBySchoolIdAndRoleId(UUID schoolId, UUID roleId);

    @Query("SELECT us FROM UserSchool us JOIN us.role r WHERE us.schoolId = :schoolId AND r.name = :roleName AND us.isActive = true")
    List<UserSchool> findBySchoolIdAndRoleName(UUID schoolId, String roleName);

    @Query("SELECT COUNT(us) FROM UserSchool us WHERE us.schoolId = :schoolId AND us.isActive = true")
    long countActiveUsersBySchoolId(UUID schoolId);
}
