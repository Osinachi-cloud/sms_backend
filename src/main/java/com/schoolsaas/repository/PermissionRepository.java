package com.schoolsaas.repository;

import com.schoolsaas.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByKey(String key);

    boolean existsByKey(String key);

    @Query("SELECT p FROM Permission p ORDER BY p.category, p.key")
    List<Permission> findAllOrderedByCategory();

    @Query("SELECT p FROM Permission p WHERE p.category = :category ORDER BY p.key")
    List<Permission> findByCategory(String category);

    @Query("SELECT DISTINCT p.category FROM Permission p ORDER BY p.category")
    List<String> findAllCategories();

    @Query("SELECT p.key FROM Permission p WHERE p.key IN :keys")
    Set<String> findExistingKeys(Set<String> keys);
}
