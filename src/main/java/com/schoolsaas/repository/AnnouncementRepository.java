package com.schoolsaas.repository;

import com.schoolsaas.model.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    Page<Announcement> findBySchoolIdOrderByIsPinnedDescCreatedAtDesc(UUID schoolId, Pageable pageable);

    List<Announcement> findBySchoolIdAndTargetAudienceAndExpiresAtAfterOrderByIsPinnedDescCreatedAtDesc(UUID schoolId, String targetAudience, LocalDateTime now);

    Page<Announcement> findBySchoolIdAndTargetAudienceAndExpiresAtAfterOrderByIsPinnedDescCreatedAtDesc(UUID schoolId, String targetAudience, LocalDateTime now, Pageable pageable);

    List<Announcement> findBySchoolIdAndExpiresAtAfterOrExpiresAtIsNullOrderByIsPinnedDescCreatedAtDesc(UUID schoolId, LocalDateTime now);

    Page<Announcement> findBySchoolIdAndExpiresAtAfterOrExpiresAtIsNullOrderByIsPinnedDescCreatedAtDesc(UUID schoolId, LocalDateTime now, Pageable pageable);
}
