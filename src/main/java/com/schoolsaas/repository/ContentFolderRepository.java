package com.schoolsaas.repository;

import com.schoolsaas.model.ContentFolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentFolderRepository extends JpaRepository<ContentFolder, UUID> {

    List<ContentFolder> findBySchoolIdOrderBySortOrderAsc(UUID schoolId);

    Page<ContentFolder> findBySchoolIdOrderBySortOrderAsc(UUID schoolId, Pageable pageable);

    List<ContentFolder> findBySchoolIdAndParentIdIsNullOrderBySortOrderAsc(UUID schoolId);

    Page<ContentFolder> findBySchoolIdAndParentIdIsNullOrderBySortOrderAsc(UUID schoolId, Pageable pageable);

    List<ContentFolder> findByParentIdOrderBySortOrderAsc(UUID parentId);

    @Query("SELECT f FROM ContentFolder f WHERE f.schoolId = :schoolId AND f.classId = :classId ORDER BY f.sortOrder")
    List<ContentFolder> findBySchoolIdAndClassId(UUID schoolId, UUID classId);

    @Query("SELECT f FROM ContentFolder f WHERE f.schoolId = :schoolId AND f.subjectId = :subjectId ORDER BY f.sortOrder")
    List<ContentFolder> findBySchoolIdAndSubjectId(UUID schoolId, UUID subjectId);

    boolean existsBySchoolIdAndNameAndParentId(UUID schoolId, String name, UUID parentId);
}
