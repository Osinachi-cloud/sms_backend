package com.schoolsaas.service;

import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.dto.library.LibraryBookDto;
import com.schoolsaas.model.BookBorrowal;
import com.schoolsaas.model.LibraryBook;
import com.schoolsaas.model.User;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.SecurityUtils;
import com.schoolsaas.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final LibraryBookRepository bookRepository;
    private final BookBorrowalRepository borrowalRepository;
    private final BookCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserSchoolRepository userSchoolRepository;

    @Transactional
    public LibraryBookDto addBook(UUID schoolId, LibraryBookDto dto) {
        log.info("Adding new book: {} to school: {}", dto.getTitle(), schoolId);
        LibraryBook book = LibraryBook.builder()
                .schoolId(schoolId)
                .categoryId(dto.getCategoryId())
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .isbn(dto.getIsbn())
                .publisher(dto.getPublisher())
                .edition(dto.getEdition())
                .description(dto.getDescription())
                .coverImageUrl(dto.getCoverImageUrl())
                .fileUrl(dto.getFileUrl())
                .fileType(dto.getFileType())
                .totalCopies(dto.getTotalCopies())
                .availableCopies(dto.getAvailableCopies())
                .isDigital(dto.getIsDigital())
                .tags(dto.getTags())
                .audienceRoles(dto.getAudienceRoles())
                .metadata(dto.getMetadata())
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();
        book = bookRepository.save(book);
        return mapToDto(book);
    }

    public Page<LibraryBookDto> listBooks(UUID schoolId, Pageable pageable) {
        Page<LibraryBook> books = bookRepository.findBySchoolId(schoolId, pageable);
        log.info("Found {} books in repository for school {}", books.getTotalElements(), schoolId);
        
        // Get current user and their role in THIS school
        UserPrincipal principal = SecurityUtils.getCurrentUser().orElse(null);
        if (principal == null) {
            log.warn("No authenticated user found while listing books");
            return books.map(this::mapToDto);
        }

        if (principal.isPlatformAdmin()) {
            log.info("User {} is platform admin, bypassing audience filter", principal.getId());
            return books.map(this::mapToDto);
        }

        // Check if the user's current school matches the requested school
        String role;
        if (schoolId.equals(principal.getCurrentSchoolId())) {
            role = principal.getSchoolRole();
            log.info("Using schoolRole from principal: {}", role);
        } else {
            // If they are accessing another school's library, we need to check their role there
            role = userSchoolRepository.findByUserIdAndSchoolId(principal.getId(), schoolId)
                    .map(us -> us.getRole() != null ? us.getRole().getName() : "")
                    .orElse("");
            log.info("Fetched schoolRole from DB for user {} and school {}: {}", principal.getId(), schoolId, role);
        }

        if (role == null) role = "";

        if (isStaffRole(role)) {
            log.info("User role {} is a staff role, bypassing audience filter", role);
            return books.map(this::mapToDto);
        }

        String finalRole = role;
        List<LibraryBookDto> filtered = books.getContent().stream()
                .filter(b -> isVisibleToRole(b, finalRole))
                .map(this::mapToDto)
                .collect(Collectors.toList());

        log.info("Filtered {} active books to {} based on role {}", books.getNumberOfElements(), filtered.size(), role);

        return new PageImpl<>(filtered, pageable, books.getTotalElements());
    }

    private boolean isStaffRole(String role) {
        if (role == null) return false;
        String upperRole = role.toUpperCase();
        return upperRole.equals("GENERAL_ADMIN") || 
               upperRole.equals("SCHOOL_ADMIN") || 
               upperRole.equals("ADMIN") || 
               upperRole.equals("TEACHER") || 
               upperRole.equals("LIBRARIAN");
    }

    private boolean isVisibleToRole(LibraryBook book, String role) {
        if (book.getAudienceRoles() == null || book.getAudienceRoles().length == 0) {
            return true; // Visible to everyone if no audience specified
        }
        if (role == null || role.isEmpty()) return false;
        
        String upperRole = role.toUpperCase();
        return Arrays.stream(book.getAudienceRoles())
                .anyMatch(r -> r != null && r.toUpperCase().equals(upperRole));
    }

    public List<LibraryBookDto> searchBooks(UUID schoolId, String query) {
        List<LibraryBook> books = bookRepository.searchBySchoolId(schoolId, query);
        log.info("Found {} books matching query '{}' in school {}", books.size(), query, schoolId);
        
        // Get current user and their role in THIS school
        UserPrincipal principal = SecurityUtils.getCurrentUser().orElse(null);
        if (principal == null) return books.stream().map(this::mapToDto).collect(Collectors.toList());

        if (principal.isPlatformAdmin()) {
            return books.stream().map(this::mapToDto).collect(Collectors.toList());
        }

        String role;
        if (schoolId.equals(principal.getCurrentSchoolId())) {
            role = principal.getSchoolRole();
        } else {
            role = userSchoolRepository.findByUserIdAndSchoolId(principal.getId(), schoolId)
                    .map(us -> us.getRole() != null ? us.getRole().getName() : "")
                    .orElse("");
        }

        if (role == null) role = "";

        if (isStaffRole(role)) {
            return books.stream().map(this::mapToDto).collect(Collectors.toList());
        }

        String finalRole = role;
        return books.stream()
                .filter(b -> isVisibleToRole(b, finalRole))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookBorrowal borrowBook(UUID bookId, UUID userId) {
        LibraryBook book = bookRepository.findById(bookId).orElseThrow();
        if (!book.getIsDigital() && book.getAvailableCopies() <= 0) {
            throw new RuntimeException("Book not available");
        }
        if (!book.getIsDigital()) {
            book.setAvailableCopies(book.getAvailableCopies() - 1);
            bookRepository.save(book);
        }
        BookBorrowal borrowal = BookBorrowal.builder()
                .bookId(bookId)
                .userId(userId)
                .dueDate(LocalDate.now().plusDays(14))
                .build();
        return borrowalRepository.save(borrowal);
    }

    @Transactional
    public BookBorrowal returnBook(UUID borrowalId) {
        BookBorrowal borrowal = borrowalRepository.findById(borrowalId).orElseThrow();
        borrowal.setStatus("RETURNED");
        borrowal.setReturnDate(LocalDate.now());

        LibraryBook book = bookRepository.findById(borrowal.getBookId()).orElse(null);
        if (book != null && !book.getIsDigital()) {
            book.setAvailableCopies(Math.min(book.getAvailableCopies() + 1, book.getTotalCopies()));
            bookRepository.save(book);
        }
        return borrowalRepository.save(borrowal);
    }

    public void deleteBook(UUID schoolId, UUID bookId) {
        LibraryBook book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));
        
        if (!book.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized: Book does not belong to this school");
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isPlatformAdmin = SecurityUtils.isPlatformAdmin();
        
        String role = userSchoolRepository.findByUserIdAndSchoolId(currentUserId, schoolId)
                .map(us -> us.getRole() != null ? us.getRole().getName() : "")
                .orElse("");
        
        boolean isSchoolAdmin = role.equals("SCHOOL_ADMIN") || role.equals("GENERAL_ADMIN");
        boolean isCreator = book.getCreatedBy() != null && book.getCreatedBy().equals(currentUserId);

        if (!isPlatformAdmin && !isSchoolAdmin && !isCreator) {
            throw new RuntimeException("Unauthorized: You can only delete your own books");
        }

        bookRepository.delete(book);
    }

    private LibraryBookDto mapToDto(LibraryBook book) {
        LibraryBookDto dto = new LibraryBookDto();
        dto.setId(book.getId());
        dto.setCategoryId(book.getCategoryId());
        if (book.getCategoryId() != null) {
            categoryRepository.findById(book.getCategoryId()).ifPresent(c -> dto.setCategoryName(c.getName()));
        }
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setIsbn(book.getIsbn());
        dto.setPublisher(book.getPublisher());
        dto.setEdition(book.getEdition());
        dto.setDescription(book.getDescription());
        dto.setCoverImageUrl(book.getCoverImageUrl());
        dto.setFileUrl(book.getFileUrl());
        dto.setFileType(book.getFileType());
        dto.setTotalCopies(book.getTotalCopies());
        dto.setAvailableCopies(book.getAvailableCopies());
        dto.setIsDigital(book.getIsDigital());
        dto.setTags(book.getTags());
        dto.setAudienceRoles(book.getAudienceRoles());
        dto.setMetadata(book.getMetadata());
        dto.setCreatedBy(book.getCreatedBy());
        dto.setCreatedAt(book.getCreatedAt());
        return dto;
    }
}
