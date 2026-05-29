package com.schoolsaas.repository;

import com.schoolsaas.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByRoleId(UUID roleId);

    @Query("SELECT rp.permissionKey FROM RolePermission rp WHERE rp.roleId = :roleId")
    Set<String> findPermissionKeysByRoleId(UUID roleId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId")
    void deleteByRoleId(UUID roleId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId AND rp.permissionKey IN :keys")
    void deleteByRoleIdAndPermissionKeys(UUID roleId, Set<String> keys);

    boolean existsByRoleIdAndPermissionKey(UUID roleId, String permissionKey);
}
