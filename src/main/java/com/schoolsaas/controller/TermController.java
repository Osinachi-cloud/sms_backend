package com.schoolsaas.controller;

import com.schoolsaas.model.Term;
import com.schoolsaas.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermRepository termRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Term>> getTerms(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(termRepository.findBySchoolIdOrderByStartDateDesc(schoolId, pageable));
    }

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Term> getCurrentTerm(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(
                termRepository.findBySchoolIdAndIsCurrentTrue(schoolId).orElse(null)
        );
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Term>> getTermsBySession(@PathVariable UUID schoolId, @PathVariable UUID sessionId, Pageable pageable) {
        return ResponseEntity.ok(termRepository.findBySessionId(sessionId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Term> createTerm(@PathVariable UUID schoolId, @RequestBody Term term) {
        term.setSchoolId(schoolId);
        return ResponseEntity.ok(termRepository.save(term));
    }

    @PutMapping("/{termId}")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Term> updateTerm(
            @PathVariable UUID schoolId,
            @PathVariable UUID termId,
            @RequestBody Term term) {
        Term existing = termRepository.findById(termId)
                .orElseThrow(() -> new RuntimeException("Term not found"));
        if (!existing.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Term not found");
        }
        existing.setName(term.getName());
        existing.setStartDate(term.getStartDate());
        existing.setEndDate(term.getEndDate());
        existing.setIsCurrent(term.getIsCurrent());
        existing.setSessionId(term.getSessionId());
        return ResponseEntity.ok(termRepository.save(existing));
    }

    @DeleteMapping("/{termId}")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteTerm(@PathVariable UUID schoolId, @PathVariable UUID termId) {
        Term existing = termRepository.findById(termId)
                .orElseThrow(() -> new RuntimeException("Term not found"));
        if (!existing.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Term not found");
        }
        termRepository.delete(existing);
        return ResponseEntity.ok().build();
    }
}
