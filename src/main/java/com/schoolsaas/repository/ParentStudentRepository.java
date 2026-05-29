package com.schoolsaas.repository;

import com.schoolsaas.model.ParentStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParentStudentRepository extends JpaRepository<ParentStudent, UUID> {
    List<ParentStudent> findByParentId(UUID parentId);
    List<ParentStudent> findByStudentId(UUID studentId);
}
