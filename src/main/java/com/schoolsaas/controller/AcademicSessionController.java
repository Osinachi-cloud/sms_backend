package com.schoolsaas.controller;

import com.schoolsaas.model.AcademicSession;
import com.schoolsaas.repository.AcademicSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/sessions")
@RequiredArgsConstructor
public class AcademicSessionController {

    private final AcademicSessionRepository sessionRepository;

    @GetMapping
    @PreAuthorize("hasPermission(#schoolId, 'school.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Page<AcademicSession>> getSessions(@PathVariable UUID schoolId, Pageable pageable) {
        return ResponseEntity.ok(sessionRepository.findBySchoolId(schoolId, pageable));
    }

    @GetMapping("/current")
    @PreAuthorize("hasPermission(#schoolId, 'school.read') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<AcademicSession> getCurrentSession(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(
                sessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId).orElse(null)
        );
    }

    @PostMapping
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<AcademicSession> createSession(@PathVariable UUID schoolId, @RequestBody AcademicSession session) {
        session.setSchoolId(schoolId);
        return ResponseEntity.ok(sessionRepository.save(session));
    }

    @PutMapping("/{sessionId}")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<AcademicSession> updateSession(
            @PathVariable UUID schoolId,
            @PathVariable UUID sessionId,
            @RequestBody AcademicSession session) {
        AcademicSession existing = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!existing.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Session not found");
        }
        existing.setName(session.getName());
        existing.setStartDate(session.getStartDate());
        existing.setEndDate(session.getEndDate());
        existing.setIsCurrent(session.getIsCurrent());
        return ResponseEntity.ok(sessionRepository.save(existing));
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasPermission(#schoolId, 'school.update') or hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID schoolId, @PathVariable UUID sessionId) {
        AcademicSession existing = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!existing.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Session not found");
        }
        sessionRepository.delete(existing);
        return ResponseEntity.ok().build();
    }
}
