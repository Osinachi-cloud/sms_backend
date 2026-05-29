package com.schoolsaas.repository;

import com.schoolsaas.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findBySchoolIdAndIsActiveTrue(UUID schoolId);

    Optional<Role> findBySchoolIdAndName(UUID schoolId, String name);

    boolean existsBySchoolIdAndName(UUID schoolId, String name);

    @Query("SELECT r FROM Role r WHERE r.schoolId = :schoolId AND r.isSystemRole = true")
    List<Role> findSystemRolesBySchoolId(UUID schoolId);

    @Query("SELECT r FROM Role r WHERE r.schoolId = :schoolId AND r.name = :name AND r.isActive = true")
    Optional<Role> findActiveBySchoolIdAndName(UUID schoolId, String name);
}
