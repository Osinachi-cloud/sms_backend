package com.schoolsaas.repository;

import com.schoolsaas.model.CourseContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseContentRepository extends JpaRepository<CourseContent, UUID> {

    List<CourseContent> findBySchoolIdAndSubjectIdAndStatusOrderByWeekNumberAsc(UUID schoolId, UUID subjectId, String status);

    Page<CourseContent> findBySchoolIdAndSubjectIdAndStatusOrderByWeekNumberAsc(UUID schoolId, UUID subjectId, String status, Pageable pageable);

    List<CourseContent> findBySchoolIdAndClassIdAndStatusOrderByWeekNumberAsc(UUID schoolId, UUID classId, String status);

    Page<CourseContent> findBySchoolIdAndClassIdAndStatusOrderByWeekNumberAsc(UUID schoolId, UUID classId, String status, Pageable pageable);

    List<CourseContent> findBySchoolIdAndTeacherIdOrderByCreatedAtDesc(UUID schoolId, UUID teacherId);

    Page<CourseContent> findBySchoolIdAndTeacherIdOrderByCreatedAtDesc(UUID schoolId, UUID teacherId, Pageable pageable);

    List<CourseContent> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    Page<CourseContent> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    @Query("SELECT cc FROM CourseContent cc WHERE cc.schoolId = :schoolId AND cc.subjectId = :subjectId AND cc.status = 'PUBLISHED' ORDER BY cc.weekNumber ASC, cc.createdAt DESC")
    List<CourseContent> findPublishedBySubject(@Param("schoolId") UUID schoolId, @Param("subjectId") UUID subjectId);

    @Query("SELECT cc FROM CourseContent cc WHERE cc.schoolId = :schoolId AND cc.subjectId = :subjectId AND cc.status = 'PUBLISHED' ORDER BY cc.weekNumber ASC, cc.createdAt DESC")
    Page<CourseContent> findPublishedBySubject(@Param("schoolId") UUID schoolId, @Param("subjectId") UUID subjectId, Pageable pageable);

    @Query("SELECT cc FROM CourseContent cc WHERE cc.schoolId = :schoolId AND cc.classId = :classId AND cc.status = 'PUBLISHED' ORDER BY cc.weekNumber ASC, cc.createdAt DESC")
    List<CourseContent> findPublishedByClass(@Param("schoolId") UUID schoolId, @Param("classId") UUID classId);

    @Query("SELECT cc FROM CourseContent cc WHERE cc.schoolId = :schoolId AND cc.classId = :classId AND cc.status = 'PUBLISHED' ORDER BY cc.weekNumber ASC, cc.createdAt DESC")
    Page<CourseContent> findPublishedByClass(@Param("schoolId") UUID schoolId, @Param("classId") UUID classId, Pageable pageable);
}
