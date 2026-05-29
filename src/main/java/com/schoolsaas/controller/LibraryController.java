package com.schoolsaas.controller;

import com.schoolsaas.dto.library.LibraryBookDto;
import com.schoolsaas.model.BookBorrowal;
import com.schoolsaas.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @PostMapping("/books")
    public ResponseEntity<LibraryBookDto> addBook(@PathVariable UUID schoolId, @RequestBody LibraryBookDto dto) {
        return ResponseEntity.ok(libraryService.addBook(schoolId, dto));
    }

    @GetMapping("/books")
    public ResponseEntity<Page<LibraryBookDto>> listBooks(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(libraryService.listBooks(schoolId, pageable));
    }

    @GetMapping("/books/search")
    public ResponseEntity<List<LibraryBookDto>> searchBooks(@PathVariable UUID schoolId, @RequestParam String query) {
        return ResponseEntity.ok(libraryService.searchBooks(schoolId, query));
    }

    @PostMapping("/books/{bookId}/borrow")
    public ResponseEntity<BookBorrowal> borrowBook(@PathVariable UUID bookId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(libraryService.borrowBook(bookId, UUID.fromString(body.get("userId"))));
    }

    @PostMapping("/borrowals/{borrowalId}/return")
    public ResponseEntity<BookBorrowal> returnBook(@PathVariable UUID borrowalId) {
        return ResponseEntity.ok(libraryService.returnBook(borrowalId));
    }
}
