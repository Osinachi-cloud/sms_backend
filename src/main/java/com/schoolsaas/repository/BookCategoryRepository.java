package com.schoolsaas.repository;

import com.schoolsaas.model.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookCategoryRepository extends JpaRepository<BookCategory, UUID> {
    List<BookCategory> findBySchoolId(UUID schoolId);
}
