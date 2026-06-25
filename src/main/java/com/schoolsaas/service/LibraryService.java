package com.schoolsaas.service;

import com.schoolsaas.dto.library.LibraryBookDto;
import com.schoolsaas.model.BookBorrowal;
import com.schoolsaas.model.LibraryBook;
import com.schoolsaas.model.User;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
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
        Page<LibraryBook> books = bookRepository.findBySchoolIdAndIsActiveTrue(schoolId, pageable);
        
        // Filter based on role if it's not an admin
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) return books.map(this::mapToDto);

        User user = userRepository.findById(currentUserId).orElse(null);
        if (user == null) return books.map(this::mapToDto);

        UUID currentSchoolId = SecurityUtils.getCurrentSchoolId();
        if (currentSchoolId == null) return books.map(this::mapToDto);

        String role = userSchoolRepository.findByUserIdAndSchoolId(currentUserId, currentSchoolId)
                .map(us -> us.getRole() != null ? us.getRole().getName() : "")
                .orElse("");

        if (role.equals("GENERAL_ADMIN") || role.equals("SCHOOL_ADMIN") || SecurityUtils.isPlatformAdmin()) {
            return books.map(this::mapToDto);
        }

        List<LibraryBookDto> filtered = books.getContent().stream()
                .filter(b -> isVisibleToRole(b, role))
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    private boolean isVisibleToRole(LibraryBook book, String role) {
        if (book.getAudienceRoles() == null || book.getAudienceRoles().length == 0) {
            return true; // Visible to everyone if no audience specified
        }
        return Arrays.asList(book.getAudienceRoles()).contains(role);
    }

    public List<LibraryBookDto> searchBooks(UUID schoolId, String query) {
        List<LibraryBook> books = bookRepository.findBySchoolIdAndTitleStartingWithIgnoreCaseAndIsActiveTrue(schoolId, query);
        
        // Filter based on role if it's not an admin
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) return books.stream().map(this::mapToDto).collect(Collectors.toList());

        User user = userRepository.findById(currentUserId).orElse(null);
        if (user == null) return books.stream().map(this::mapToDto).collect(Collectors.toList());

        UUID currentSchoolId = SecurityUtils.getCurrentSchoolId();
        if (currentSchoolId == null) return books.stream().map(this::mapToDto).collect(Collectors.toList());

        String role = userSchoolRepository.findByUserIdAndSchoolId(currentUserId, currentSchoolId)
                .map(us -> us.getRole() != null ? us.getRole().getName() : "")
                .orElse("");

        if (role.equals("GENERAL_ADMIN") || role.equals("SCHOOL_ADMIN") || SecurityUtils.isPlatformAdmin()) {
            return books.stream().map(this::mapToDto).collect(Collectors.toList());
        }

        return books.stream()
                .filter(b -> isVisibleToRole(b, role))
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
                .orElseThrow(() -> new RuntimeException("Book not found"));
        if (!book.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        book.setIsActive(false);
        bookRepository.save(book);
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
        dto.setCreatedAt(book.getCreatedAt());
        return dto;
    }
}
