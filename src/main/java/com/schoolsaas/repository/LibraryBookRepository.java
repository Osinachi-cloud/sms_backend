package com.schoolsaas.repository;

import com.schoolsaas.model.LibraryBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LibraryBookRepository extends JpaRepository<LibraryBook, UUID> {
    Page<LibraryBook> findBySchoolIdAndIsActiveTrue(UUID schoolId, Pageable pageable);
    List<LibraryBook> findBySchoolIdAndCategoryIdAndIsActiveTrue(UUID schoolId, UUID categoryId);
    /**
     * Prefix search on book title (case-insensitive).
     * Uses <code>title%</code> so a B-tree index can be used.
     * For true substring search add a PostgreSQL <code>pg_trgm</code> GIN index.
     */
    List<LibraryBook> findBySchoolIdAndTitleStartingWithIgnoreCaseAndIsActiveTrue(UUID schoolId, String title);
}
