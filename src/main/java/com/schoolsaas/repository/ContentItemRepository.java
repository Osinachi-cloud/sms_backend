package com.schoolsaas.repository;

import com.schoolsaas.model.ContentItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {

    Page<ContentItem> findBySchoolIdAndStatus(UUID schoolId, String status, Pageable pageable);

    Page<ContentItem> findBySchoolId(UUID schoolId, Pageable pageable);

    List<ContentItem> findByFolderId(UUID folderId);

    Page<ContentItem> findBySchoolIdAndFolderId(UUID schoolId, UUID folderId, Pageable pageable);

    @Query("SELECT c FROM ContentItem c WHERE c.schoolId = :schoolId AND c.subjectId = :subjectId ORDER BY c.createdAt DESC")
    Page<ContentItem> findBySchoolIdAndSubjectId(UUID schoolId, UUID subjectId, Pageable pageable);

    @Query("SELECT c FROM ContentItem c WHERE c.schoolId = :schoolId AND c.folderId IS NULL ORDER BY c.createdAt DESC")
    Page<ContentItem> findUnfolderedBySchoolId(UUID schoolId, Pageable pageable);

    Page<ContentItem> findByTeacherId(UUID teacherId, Pageable pageable);

    @Query("SELECT c FROM ContentItem c WHERE c.schoolId = :schoolId AND c.status = 'PENDING' ORDER BY c.createdAt DESC")
    Page<ContentItem> findPendingBySchoolId(UUID schoolId, Pageable pageable);

    @Query("SELECT c FROM ContentItem c WHERE c.schoolId = :schoolId AND c.status IN ('APPROVED', 'PUBLISHED') ORDER BY c.publishedAt DESC")
    Page<ContentItem> findPublishedBySchoolId(UUID schoolId, Pageable pageable);

    @Query("SELECT c FROM ContentItem c WHERE c.teacherId = :teacherId AND c.status = :status")
    Page<ContentItem> findByTeacherIdAndStatus(UUID teacherId, String status, Pageable pageable);

    @Query("SELECT COUNT(c) FROM ContentItem c WHERE c.schoolId = :schoolId AND c.status = 'PENDING'")
    long countPendingBySchoolId(UUID schoolId);

    @Query("SELECT COUNT(c) FROM ContentItem c WHERE c.teacherId = :teacherId AND c.status = :status")
    long countByTeacherIdAndStatus(UUID teacherId, String status);

    List<ContentItem> findByStatusAndScheduledPublishAtBefore(String status, LocalDateTime dateTime);

    List<ContentItem> findBySchoolId(UUID schoolId);

    @Query("SELECT c FROM ContentItem c WHERE c.schoolId = :schoolId ORDER BY c.createdAt DESC")
    Page<ContentItem> findRecentBySchoolId(UUID schoolId, org.springframework.data.domain.Pageable pageable);

    long countBySchoolId(UUID schoolId);

    @Query("SELECT c FROM ContentItem c WHERE c.schoolId = :schoolId AND c.featured = true AND c.status = 'PUBLISHED'")
    List<ContentItem> findFeaturedBySchoolId(UUID schoolId);

    long countBySchoolIdAndStatus(UUID schoolId, String status);
}
