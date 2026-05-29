package com.schoolsaas.repository;

import com.schoolsaas.model.ContentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentVersionRepository extends JpaRepository<ContentVersion, UUID> {

    Page<ContentVersion> findByContentIdOrderByVersionNumberDesc(UUID contentId, Pageable pageable);

    Optional<ContentVersion> findByContentIdAndVersionNumber(UUID contentId, Integer versionNumber);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM ContentVersion v WHERE v.contentId = :contentId")
    Integer findMaxVersionNumber(UUID contentId);

    long countByContentId(UUID contentId);
}
