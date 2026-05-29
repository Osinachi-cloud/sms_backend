package com.schoolsaas.repository;

import com.schoolsaas.model.TeacherClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherClassRepository extends JpaRepository<TeacherClass, UUID> {

    List<TeacherClass> findByTeacherId(UUID teacherId);

    List<TeacherClass> findByTeacherIdAndSessionId(UUID teacherId, UUID sessionId);

    List<TeacherClass> findByClassId(UUID classId);

    @Query("SELECT tc FROM TeacherClass tc WHERE tc.teacher.schoolId = :schoolId AND tc.teacherId = :teacherId")
    List<TeacherClass> findBySchoolIdAndTeacherId(@Param("schoolId") UUID schoolId, @Param("teacherId") UUID teacherId);

    @Query("SELECT DISTINCT tc.classId FROM TeacherClass tc WHERE tc.teacherId = :teacherId")
    List<UUID> findClassIdsByTeacherId(@Param("teacherId") UUID teacherId);
}
