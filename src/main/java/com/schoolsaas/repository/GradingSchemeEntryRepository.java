package com.schoolsaas.repository;

import com.schoolsaas.model.GradingSchemeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradingSchemeEntryRepository extends JpaRepository<GradingSchemeEntry, UUID> {

    List<GradingSchemeEntry> findBySchoolIdAndClassIdAndSubjectIdAndTermIdAndActiveTrue(
            UUID schoolId, UUID classId, UUID subjectId, UUID termId);

    Optional<GradingSchemeEntry> findBySchoolIdAndClassIdAndSubjectIdAndTermIdAndSourceTypeAndSourceId(
            UUID schoolId, UUID classId, UUID subjectId, UUID termId, String sourceType, UUID sourceId);

    @Modifying
    @Query("DELETE FROM GradingSchemeEntry g WHERE g.schoolId = :schoolId AND g.classId = :classId AND " +
           "g.subjectId = :subjectId AND g.termId = :termId")
    void deleteByFilters(UUID schoolId, UUID classId, UUID subjectId, UUID termId);

    @Query("SELECT COALESCE(SUM(g.weight), 0) FROM GradingSchemeEntry g WHERE g.schoolId = :schoolId AND " +
           "g.classId = :classId AND g.subjectId = :subjectId AND g.termId = :termId AND g.active = true")
    Integer sumActiveWeights(UUID schoolId, UUID classId, UUID subjectId, UUID termId);
}
