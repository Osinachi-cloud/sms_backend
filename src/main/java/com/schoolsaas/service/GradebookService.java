package com.schoolsaas.service;

import com.schoolsaas.dto.gradebook.GradebookEntryDto;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradebookService {

    private final GradebookQueryRepository gradebookQueryRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final TermRepository termRepository;
    private final AcademicSessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public Page<GradebookEntryDto> getGradebook(
            UUID schoolId,
            UUID classId,
            UUID subjectId,
            UUID studentId,
            UUID termId,
            UUID sessionId,
            String search,
            Pageable pageable) {

        // Resolve defaults
        UUID effectiveTermId = termId;
        UUID effectiveSessionId = sessionId;
        if (effectiveTermId == null) {
            effectiveTermId = termRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                    .map(Term::getId).orElse(null);
        }
        if (effectiveSessionId == null) {
            effectiveSessionId = sessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                    .map(AcademicSession::getId).orElse(null);
        }

        // Determine teacher restriction
        List<UUID> allowedClassIds = resolveTeacherClassIds(schoolId);
        if (allowedClassIds != null && allowedClassIds.isEmpty()) {
            return Page.empty(pageable);
        }
        if (classId != null && allowedClassIds != null && !allowedClassIds.contains(classId)) {
            return Page.empty(pageable);
        }

        long total = gradebookQueryRepository.countEntries(
                schoolId, classId, subjectId, studentId,
                effectiveTermId, effectiveSessionId,
                search, allowedClassIds);

        if (total == 0) {
            return Page.empty(pageable);
        }

        List<GradebookEntryDto> entries = gradebookQueryRepository.findEntries(
                schoolId, classId, subjectId, studentId,
                effectiveTermId, effectiveSessionId,
                search, allowedClassIds, pageable);

        return new PageImpl<>(entries, pageable, total);
    }

    private List<UUID> resolveTeacherClassIds(UUID schoolId) {
        if (SecurityUtils.isAppAdmin() || SecurityUtils.isGeneralAdmin()) {
            return null; // No restriction
        }
        var userOpt = SecurityUtils.getCurrentUser();
        if (userOpt.isEmpty()) return List.of();
        var user = userOpt.get();

        var teacherOpt = teacherRepository.findBySchoolIdAndUserId(schoolId, user.getId());
        if (teacherOpt.isEmpty()) {
            return null; // Not a teacher, no restriction
        }

        Teacher teacher = teacherOpt.get();
        List<TeacherClass> assignments = teacherClassRepository.findByTeacherId(teacher.getId());
        return assignments.stream()
                .map(TeacherClass::getClassId)
                .distinct()
                .collect(Collectors.toList());
    }
}
