package com.schoolsaas.repository;

import com.schoolsaas.model.LibraryBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LibraryBookRepository extends JpaRepository<LibraryBook, UUID> {
    @Query("SELECT b FROM LibraryBook b WHERE b.schoolId = :schoolId")
    Page<LibraryBook> findBySchoolId(UUID schoolId, Pageable pageable);

    @Query("SELECT b FROM LibraryBook b WHERE b.schoolId = :schoolId AND LOWER(b.title) LIKE LOWER(CONCAT(:title, '%'))")
    List<LibraryBook> searchBySchoolId(UUID schoolId, String title);

    List<LibraryBook> findBySchoolIdAndCategoryId(UUID schoolId, UUID categoryId);
    
    List<LibraryBook> findBySchoolIdAndTitleStartingWithIgnoreCase(UUID schoolId, String title);
}
